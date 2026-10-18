package com.example.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.IpoApplicationEntity
import com.example.data.local.entity.IpoEntity
import com.example.data.local.entity.StatusEventEntity

data class ApplicationWithDetails(
    @Embedded val application: IpoApplicationEntity,
    @Relation(
        parentColumn = "ipoId",
        entityColumn = "id"
    )
    val ipo: IpoEntity,
    @Relation(
        parentColumn = "dematAccountId",
        entityColumn = "id"
    )
    val dematAccount: DematAccountEntity,
    @Relation(
        parentColumn = "familyMemberId",
        entityColumn = "id"
    )
    val familyMember: FamilyMemberEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "applicationId"
    )
    val statusEvents: List<StatusEventEntity> = emptyList()
)
