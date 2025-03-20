package de.deutschepost.sdm.cdlib.names.git

import io.kotest.core.annotation.BeforeAll
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.mockk.every
import io.mockk.mockkObject

@RequiresTag("UnitTest")
@Tags("UnitTest")
class RepositoryTest : AnnotationSpec() {
    private val currDir = System.getProperty("user.dir")

    @BeforeAll
    fun initMocks() {
        mockkObject(GitRepository)
        every { GitRepository.getRemoteUrl(any()) } returns "https://git.dhl.com/CDLib/CDlib.git"
        every { GitRepository.lastBranchCommit(any()) } returns GitRevision(
            id = "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
            longMessage = "Dummy Commit",
            shortMessage = "Dummy Commit",
            class RepositoryTest : AnnotationSpec() {
                companion object {
                    const val AUTHOR_EMAIL = "f.l@dhl.com"
                }
                private val currDir = System.getProperty("user.dir")
            
                @BeforeAll
                fun initMocks() {
                    mockkObject(GitRepository)
                    every { GitRepository.getRemoteUrl(any()) } returns "https://git.dhl.com/CDLib/CDlib.git"
                    every { GitRepository.lastBranchCommit(any()) } returns GitRevision(
                        id = "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
                        longMessage = "Dummy Commit",
                        shortMessage = "Dummy Commit",
                        authorName = "Firstname Lastname",
                        authorEmail = AUTHOR_EMAIL,
                        committerName = "Firstname Lastname",
                        committerEmail = AUTHOR_EMAIL
                    )
                }
    }

    @Test
    fun testRemoteUrl() {
        val remote = GitRepository.getRemoteUrl(currDir)
        remote shouldContainIgnoringCase "https://git.dhl.com/"
    }
}
