package de.deutschepost.sdm.cdlib.change.changemanagement.model

data class GetChangeCommentsRequest(
    val comments: List<Comment>,
    val maxResults: Int,
    val startAt: Int,
    val total: Int
) {
    data class Comment(
        val author: Author,
        val body: String,
        val created: String,
        val id: String,
        val self: String,
    ) {
        data class Author(val name: String)
    }
}

data class ChangeComment(
    val author: String,
    val comment: String
)
