package com.example.ui.components

import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/** What the fill script managed to do on the registrar's form. */
data class RegistrarFillReport(
    val companySelected: Boolean = false,
    val panOptionSelected: Boolean = false,
    val panFilled: Boolean = false,
    val captchaPresent: Boolean = false,
    val submitted: Boolean = false
)

/**
 * Builds the script that fills a registrar's allotment form. It works by
 * recognising the usual form parts rather than fixed element ids, so it covers
 * KFin, MUFG Intime, Bigshare, Cameo, Skyline, Maashitla, Purva, Integrated
 * and MAS alike:
 *  - the company dropdown option that best matches the IPO name,
 *  - the "PAN" radio / search-type option,
 *  - the PAN text field (never a CAPTCHA or OTP field),
 *  - the submit button, clicked when [autoSubmit] is set. When a CAPTCHA is
 *    on screen it is never solved or skipped: the person types it and the
 *    form is submitted for them once they pause.
 */
fun buildRegistrarFillJs(pan: String, ipoName: String, autoSubmit: Boolean): String {
    val safePan = pan.uppercase().filter { it.isLetterOrDigit() }.take(10)
    val safeIpo = ipoName.filter { it.isLetterOrDigit() || it == ' ' || it == '&' || it == '.' || it == '-' }.take(80)
    return """
        (function(pan, ipo, autoSubmit) {
          try {
            var out = {company:false, radio:false, pan:false, captcha:false, submitted:false};
            function visible(el) { return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length); }
            function meta(el) {
              var label = '';
              if (el.id) { var lb = document.querySelector('label[for="' + el.id + '"]'); if (lb) label = lb.innerText; }
              if (!label && el.parentElement) label = (el.parentElement.innerText || '').slice(0, 60);
              return ((el.id || '') + ' ' + (el.name || '') + ' ' + (el.placeholder || '') + ' ' +
                (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('formcontrolname') || '') + ' ' +
                (typeof el.className === 'string' ? el.className : '') + ' ' + label).toLowerCase();
            }
            function fire(el) {
              ['input', 'change', 'blur'].forEach(function(t) { el.dispatchEvent(new Event(t, {bubbles: true})); });
            }
            function setValue(el, v) {
              var proto = el.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype;
              var d = Object.getOwnPropertyDescriptor(proto, 'value');
              if (d && d.set) d.set.call(el, v); else el.value = v;
              fire(el);
            }

            var words = ipo.toLowerCase()
              .replace(/\b(limited|ltd|ipo|sme|india|the|and|of|private|pvt)\b/g, ' ')
              .split(/[^a-z0-9]+/).filter(function(w) { return w.length > 1; });
            var selects = document.querySelectorAll('select');
            for (var i = 0; i < selects.length && words.length; i++) {
              var s = selects[i];
              if (!visible(s)) continue;
              var best = -1, score = 0;
              for (var j = 0; j < s.options.length; j++) {
                var t = (s.options[j].text || '').toLowerCase(), sc = 0;
                words.forEach(function(w) { if (t.indexOf(w) !== -1) sc++; });
                if (sc > score) { score = sc; best = j; }
              }
              if (best >= 0 && score >= Math.min(2, words.length)) {
                s.selectedIndex = best; setValue(s, s.options[best].value); out.company = true; break;
              }
            }

            var radios = document.querySelectorAll('input[type=radio]');
            for (var k = 0; k < radios.length; k++) {
              var r = radios[k];
              if (/\bpan\b|pan no|pan card|panno|rbpan|pan_/.test(meta(r)) || /^(pan|pn)$/i.test(r.value || '')) {
                r.click(); r.checked = true; fire(r); out.radio = true; break;
              }
            }
            var typeSelects = document.querySelectorAll('select');
            for (var q = 0; q < typeSelects.length && !out.radio; q++) {
              var ts = typeSelects[q];
              for (var o = 0; o < ts.options.length; o++) {
                if (/^\s*pan\b/i.test(ts.options[o].text || '')) {
                  ts.selectedIndex = o; setValue(ts, ts.options[o].value); out.radio = true; break;
                }
              }
            }

            var inputs = document.querySelectorAll('input[type=text], input:not([type]), input[type=search], input[type=tel]');
            var target = null;
            for (var m = 0; m < inputs.length; m++) {
              var e = inputs[m];
              if (!visible(e)) continue;
              var mt = meta(e);
              if (/captcha|otp|verification code/.test(mt)) continue;
              if (/\bpan\b|pan no|panno|pan number|txtpan|pan_/.test(mt)) { target = e; break; }
            }
            if (!target && out.radio) {
              for (var n = 0; n < inputs.length; n++) {
                var e2 = inputs[n];
                if (visible(e2) && !/captcha|otp|search|email|mobile|phone|name|dp id|client/.test(meta(e2))) { target = e2; break; }
              }
            }
            if (target) { target.focus(); setValue(target, pan); out.pan = true; }

            // A CAPTCHA counts only when one is actually on screen; the word in scripts is ignored
            function visibleCaptcha() {
              var fields = document.querySelectorAll('input');
              for (var c = 0; c < fields.length; c++) {
                if (visible(fields[c]) && /captcha|security code|verification code|enter code|image code/.test(meta(fields[c]))) return fields[c];
              }
              var widgets = document.querySelectorAll('img, iframe, canvas, .g-recaptcha, .h-captcha, .cf-turnstile');
              for (var w = 0; w < widgets.length; w++) {
                var wd = widgets[w];
                var sig = ((wd.src || '') + ' ' + (wd.id || '') + ' ' + (wd.alt || '') + ' ' +
                  (typeof wd.className === 'string' ? wd.className : '')).toLowerCase();
                if (visible(wd) && /captcha|recaptcha|turnstile/.test(sig)) return wd;
              }
              return null;
            }
            function findSubmit() {
              var scope = (target && target.form) || document;
              var buttons = scope.querySelectorAll('button, input[type=submit], input[type=button], input[type=image], [role=button], a.btn');
              for (var b = 0; b < buttons.length; b++) {
                var bt = buttons[b];
                if (!visible(bt) || bt.disabled) continue;
                var text = ((bt.innerText || bt.value || bt.alt || '') + ' ' + (bt.id || '') + ' ' + (bt.name || '')).toLowerCase();
                if (/submit|search|check|get|show|\bgo\b|proceed|view|status|fetch|find/.test(text) &&
                    !/reset|clear|cancel|back|refresh|reload|download|print/.test(text)) return bt;
              }
              return null;
            }
            function submitNow() {
              var bt = findSubmit();
              if (bt) { bt.click(); return true; }
              if (target && target.form) {
                if (target.form.requestSubmit) target.form.requestSubmit(); else target.form.submit();
                return true;
              }
              return false;
            }

            var captcha = visibleCaptcha();
            out.captcha = !!captcha;
            if (autoSubmit && out.pan && !captcha) {
              out.submitted = submitNow();
            } else if (autoSubmit && out.pan && captcha && !window.__ipoOneAutoSubmitArmed) {
              // The person solves the CAPTCHA; the form is sent for them once they stop typing
              window.__ipoOneAutoSubmitArmed = true;
              if (captcha.tagName === 'INPUT') {
                captcha.focus();
                var timer = null;
                captcha.addEventListener('input', function() {
                  clearTimeout(timer);
                  if ((captcha.value || '').trim().length >= 4) timer = setTimeout(submitNow, 1200);
                });
              } else {
                var poll = setInterval(function() {
                  var token = document.querySelector('textarea[name="g-recaptcha-response"], input[name="cf-turnstile-response"], textarea[name="h-captcha-response"]');
                  if (token && token.value) { clearInterval(poll); submitNow(); }
                }, 1000);
              }
            }
            return JSON.stringify(out);
          } catch (err) {
            return JSON.stringify({error: String(err)});
          }
        })('$safePan', '$safeIpo', $autoSubmit);
    """.trimIndent()
}

/** Visible page text, including dialogs, capped so huge pages stay cheap to read. */
const val READ_PAGE_TEXT_JS =
    "(function(){ return (document.body && document.body.innerText || '').slice(0, 30000); })();"

/** evaluateJavascript hands back a JSON-encoded string; turn it into the plain value. */
fun decodeJsString(raw: String?): String =
    if (raw == null || raw == "null") "" else runCatching { JSONArray("[$raw]").optString(0, "") }.getOrDefault("")

suspend fun WebView.evaluateJs(script: String): String = suspendCancellableCoroutine { cont ->
    evaluateJavascript(script) { value -> if (cont.isActive) cont.resume(decodeJsString(value)) }
}

fun parseFillReport(json: String): RegistrarFillReport = runCatching {
    val o = JSONObject(json)
    RegistrarFillReport(
        companySelected = o.optBoolean("company"),
        panOptionSelected = o.optBoolean("radio"),
        panFilled = o.optBoolean("pan"),
        captchaPresent = o.optBoolean("captcha"),
        submitted = o.optBoolean("submitted")
    )
}.getOrDefault(RegistrarFillReport())
