package com.example.ui.components

/**
 * Fills a broker's IPO bid form: UPI ID, quantity or lots, cut-off price.
 * On a list page it opens the matching IPO's "Apply" instead. It never
 * presses the final submit; the person confirms the bid themselves.
 */
fun buildBrokerFillJs(ipoName: String, lots: Int, shares: Int, price: Int, upi: String): String {
    val safeIpo = ipoName.filter { it.isLetterOrDigit() || it == ' ' || it == '&' || it == '.' || it == '-' }.take(80)
    val safeUpi = upi.filter { it.isLetterOrDigit() || it == '@' || it == '.' || it == '-' || it == '_' }.take(60)
    return """
        (function(ipo, lots, shares, price, upi) {
          try {
            var out = {upi:false, qty:false, price:false, cutoff:false, openedApply:false};
            function visible(el) { return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length); }
            function meta(el) {
              var label = '';
              if (el.id) { var lb = document.querySelector('label[for="' + el.id + '"]'); if (lb) label = lb.innerText; }
              if (!label && el.parentElement) label = (el.parentElement.innerText || '').slice(0, 80);
              return ((el.id || '') + ' ' + (el.name || '') + ' ' + (el.placeholder || '') + ' ' +
                (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('data-testid') || '') + ' ' + label).toLowerCase();
            }
            function setValue(el, v) {
              var d = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
              if (d && d.set) d.set.call(el, v); else el.value = v;
              ['input', 'change', 'blur'].forEach(function(t) { el.dispatchEvent(new Event(t, {bubbles: true})); });
            }
            var inputs = Array.prototype.filter.call(
              document.querySelectorAll('input:not([type=hidden]):not([type=checkbox]):not([type=radio])'), visible);
            inputs.forEach(function(el) {
              var m = meta(el);
              if (!out.upi && upi && /\bupi\b|vpa|upi id|upi handle/.test(m)) {
                // Leave a UPI the person already typed on the page alone
                if (!el.value) setValue(el, upi);
                out.upi = true;
              } else if (!out.qty && /quantity|qty|no\.? of shares|shares|lots?\b/.test(m) && !/price|amount/.test(m)) {
                setValue(el, String(/lots?\b/.test(m) && !/quantity|qty|shares/.test(m) ? lots : shares));
                out.qty = true;
              } else if (!out.price && /\bprice\b|bid price/.test(m) && !/cut.?off/.test(m)) {
                if (!el.value) setValue(el, String(price));
                out.price = true;
              }
            });
            var boxes = document.querySelectorAll('input[type=checkbox]');
            for (var i = 0; i < boxes.length; i++) {
              if (/cut.?off/.test(meta(boxes[i]))) {
                if (!boxes[i].checked) boxes[i].click();
                out.cutoff = true; break;
              }
            }
            if (!out.upi && !out.qty) {
              // List page: open "Apply" on the row or card that names this IPO
              var words = ipo.toLowerCase().replace(/\b(limited|ltd|ipo|sme|india|the|and|of)\b/g, ' ')
                .split(/[^a-z0-9]+/).filter(function(w) { return w.length > 2; });
              var buttons = document.querySelectorAll('button, a, [role=button]');
              for (var b = 0; b < buttons.length && words.length; b++) {
                var bt = buttons[b];
                if (!visible(bt) || !/^\s*(apply|apply now|bid|place bid)\s*$/i.test(bt.innerText || '')) continue;
                var box = bt, hit = false;
                for (var up = 0; up < 6 && box && !hit; up++) {
                  var t = (box.innerText || '').toLowerCase(), score = 0;
                  words.forEach(function(w) { if (t.indexOf(w) !== -1) score++; });
                  hit = score >= Math.min(2, words.length);
                  box = box.parentElement;
                }
                if (hit) { bt.click(); out.openedApply = true; break; }
              }
            }
            return JSON.stringify(out);
          } catch (err) {
            return JSON.stringify({error: String(err)});
          }
        })('$safeIpo', $lots, $shares, $price, '$safeUpi');
    """.trimIndent()
}

/** Snapshot of this site's localStorage and sessionStorage, for keeping a person's login. */
const val READ_SITE_STORAGE_JS = """
(function() {
  function dump(s) { var o = {}; try { for (var i = 0; i < s.length; i++) { var k = s.key(i); o[k] = s.getItem(k); } } catch (e) {} return o; }
  return JSON.stringify({origin: location.origin, local: dump(window.localStorage), session: dump(window.sessionStorage)});
})();
"""

/** Puts a saved storage snapshot back when the page is on the same site. Returns true when applied. */
fun buildRestoreSiteStorageJs(snapshotJson: String): String {
    val quoted = org.json.JSONObject.quote(snapshotJson)
    return """
        (function(raw) {
          try {
            var snap = JSON.parse(raw);
            if (!snap || snap.origin !== location.origin) return 'false';
            localStorage.clear(); sessionStorage.clear();
            Object.keys(snap.local || {}).forEach(function(k) { localStorage.setItem(k, snap.local[k]); });
            Object.keys(snap.session || {}).forEach(function(k) { sessionStorage.setItem(k, snap.session[k]); });
            return 'true';
          } catch (e) { return 'false'; }
        })($quoted);
    """.trimIndent()
}
