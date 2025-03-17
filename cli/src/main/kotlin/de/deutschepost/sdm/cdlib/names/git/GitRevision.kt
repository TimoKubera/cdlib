package de.deutschepost.sdm.cdlib.names.git

import org.eclipse.jgit.revwalk.RevCommit

class GitRevision(
    val id: String,
    longMessage: String,
    shortMessage: String,
    val authorName: String,
    val authorEmail: String,
    val committerName: String,
    val committerEmail: String
) {
    val longMessage: String
    val shortMessage: String

    init {
        this.longMessage = longMessage.filter { it.isLetterOrDigit() || it.isWhitespace() }
        this.shortMessage = shortMessage.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }

    companion object {
        fun of(commit: RevCommit): GitRevision {
            return GitRevision(
                id = commit.id.name,
                longMessage = commit.fullMessage,
                shortMessage = commit.shortMessage,
                authorName = commit.authorIdent.name,
                authorEmail = commit.authorIdent.emailAddress,
                committerName = commit.committerIdent.name,
                committerEmail = commit.committerIdent.emailAddress
            )
        }
    }
}
