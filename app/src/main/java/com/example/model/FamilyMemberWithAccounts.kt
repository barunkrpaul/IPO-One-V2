package com.example.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.UpiAccountEntity

data class FamilyMemberWithAccounts(
    @Embedded val member: FamilyMemberEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "familyMemberId"
    )
    val dematAccounts: List<DematAccountEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "familyMemberId"
    )
    val upiAccounts: List<UpiAccountEntity>
)

data class DematWithMember(
    @Embedded val dematAccount: DematAccountEntity,
    @Relation(
        parentColumn = "familyMemberId",
        entityColumn = "id"
    )
    val familyMember: FamilyMemberEntity
)
