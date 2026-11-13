package com.gondroid.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A frequent error the user swiped away. `question_id` holds the error's key (`errorKey` in
 * `evaluation:presentation`: bank + PDF id + text), not the bare question id, which repeats across
 * banks and inside a3b/a3c. Rows written before that hold bare ids and match nothing any more.
 */
@Entity(tableName = "dismissed_questions")
data class DismissedQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: Int
)
