package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.names.NameResolver.Companion.logger
import de.deutschepost.sdm.cdlib.names.git.GitRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

abstract class NameResolverBase(private val namesConfigWithDefault: NamesConfigWithDefault) : NameResolver {
    private val map: MutableMap<Names, String> = mutableMapOf()

    override fun seed(releaseName: String) {
        // Expect release spec. syntax <app name>_<app version>_<build no>_<revision>
        // --> derive app name / app version / revision from release spec.
        val tokens = releaseName.split('_')
        if (tokens.size >= 4) {
            override(Names.CDLIB_REVISION, tokens.last())
            override(Names.CDLIB_BUILD_NUMBER, tokens[tokens.size - 2])
            override(Names.CDLIB_APP_VERSION, tokens[tokens.size - 3])
            override(
                Names.CDLIB_APP_NAME,
                tokens.filterIndexed { i, _ -> i <= tokens.size - 4 }.joinToString(separator = "_")
            )
        } else {
            throw IllegalArgumentException("Release name not valid: $releaseName")
        }
    }

    override fun get(key: Names) = map.getOrPut(key) {
        when (key) {
            Names.CDLIB_APP_NAME -> createAppName()
            Names.CDLIB_APP_VERSION -> createAppVersion()
            Names.CDLIB_BUILD_NUMBER -> createBuildNumber()
            Names.CDLIB_CHART_VERSION -> createChartVersion()
            Names.CDLIB_CHART_VERSION_OCI -> createChartVersionOCI()
            Names.CDLIB_CONTAINER_TAG -> createContainerTag()
            Names.CDLIB_EFFECTIVE_BRANCH_NAME -> createEffectiveBranchName()
            Names.CDLIB_JOB_URL -> createJobUrl()
            Names.CDLIB_PM_GIT_ID -> createGitId()
            Names.CDLIB_PM_GIT_LINK -> createGitLink()
            Names.CDLIB_PM_GIT_MAIL -> createGitMail()
            Names.CDLIB_PM_GIT_MESSAGE -> createGitMessage()
            Names.CDLIB_PM_GIT_NAME -> createGitName()
            Names.CDLIB_PM_GIT_ORIGIN -> createGitOrigin()
            Names.CDLIB_RELEASE_NAME -> createReleaseName()
            Names.CDLIB_RELEASE_NAME_FORTIFY -> createReleaseNameFortify()
            Names.CDLIB_RELEASE_NAME_HELM -> createReleaseNameHelm()
            Names.CDLIB_RELEASE_VERSION -> createReleaseVersion()
            Names.CDLIB_RELEASE_NAME_UNIQUE -> createReleaseNameUnique()
            Names.CDLIB_REVISION -> createRevision()
            Names.CDLIB_SANITIZED_BRANCH_NAME -> createSanitizedBranchName()
            Names.CDLIB_SEMANTIC_VERSION -> createSemanticVersion()
            Names.CDLIB_TERRAFORM_PREFIX -> createTerraformPrefix()
            Names.CDLIB_CICD_PLATFORM -> createCicdPlatform()
            Names.CDLIB_PIPELINE_URL -> createPipelineUrl()
        }
    }

    override val entries: Set<Map.Entry<Names, String>> by map::entries
    override val keys: Set<Names> by map::keys
    override val values: Collection<String> by map::values
    override val size by map::size
    override fun containsKey(key: Names) = map.containsKey(key)
    override fun containsValue(value: String) = map.containsValue(value)
    override fun isEmpty() = map.isEmpty()
    override fun getOrDefault(key: Names, defaultValue: String) = map.getOrDefault(key, defaultValue)

    override fun override(name: Names, value: String) {
        map[name] = value
    }

    //
    // Internal contract for sub-class implementations
    //
    abstract fun createAppName(): String

    open fun createAppVersion(): String {
        val formatter = DateTimeFormatter.ofPattern(namesConfigWithDefault.datetimePattern)
        val zoneOffset = ZoneId.of(namesConfigWithDefault.datetimeZone)
        val timeString = ZonedDateTime.now(zoneOffset).format(formatter)
        // This will remove leading zeros as they are not semver compatible
        return timeString
            .split(".")
            .map(String::toInt)
            .joinToString(".")
    }

    open fun createChartVersion(): String =
        "${get(Names.CDLIB_APP_VERSION)}-${get(Names.CDLIB_EFFECTIVE_BRANCH_NAME)}"
            // this is the inversion of the docker tag regex https://github.com/docker/distribution/blob/master/reference/regexp.go#L37
            // this regex converts everything that is NOT a word character: ([a-zA-Z_0-9]), dash(-) or dot(.) to a dash(-)
            .replace("[^\\w.-]|_".toRegex(), "-")
            // helm chart limitation of no consecutive special
            .replace("\\W\\W+".toRegex(), "-")
            .lowercase()
            // helm chart version max length is 35 characters
            .take(35)
            // openshift limitation of no special character at the end
            .dropLastWhile { !it.isLetterOrDigit() }

    open fun createChartVersionOCI(): String {
        val helmSuffix = "-helm"
        return get(Names.CDLIB_CHART_VERSION)
            // helm chart version max length is 35 characters
            .take(35 - helmSuffix.length)
            // add the -helm suffix
            .plus(helmSuffix)
            // helm chart limitation of no consecutive special
            .replace("\\W\\W+".toRegex(), "-")
    }

    open fun createContainerTag(): String = get(Names.CDLIB_CHART_VERSION)

    abstract fun createEffectiveBranchName(): String

    open fun createGitId(): String = gitRevision.id

    open fun createGitLink(): String {
        val origin = get(Names.CDLIB_PM_GIT_ORIGIN)
        val gitId = get(Names.CDLIB_PM_GIT_ID)
        return when {
            origin.contains("git.dhl.com") -> {
                "${origin.substringBefore(".git")}/commit/$gitId"
            }
            origin.contains("dev.azure.com") -> "$origin/commit/$gitId"
            origin.contains("gitlab.com") -> "${origin.substringBefore(".git")}/-/commit/$gitId"
            else -> origin
        }
    }

    open fun createGitMail(): String =
        gitRevision.committerEmail.ifBlank {
            "unknownMail"
        }

    open fun createGitMessage(): String = gitRevision.shortMessage

    open fun createGitName(): String = gitRevision.committerName

    open fun createGitOrigin(): String =
        namesConfigWithDefault.originOverride ?: run {
            val currentDir = namesConfigWithDefault.repoPath
            GitRepository.getRemoteUrl(currentDir)
        }

    abstract fun createJobUrl(): String

    open fun createReleaseName(): String {
        return "${get(Names.CDLIB_APP_NAME)}_${get(Names.CDLIB_RELEASE_VERSION)}"
    }

    open fun createReleaseNameFortify(): String {
        return get(Names.CDLIB_RELEASE_NAME).replace("[^A-Za-z0-9]".toRegex(), "_")
    }

    open fun createReleaseNameHelm(): String {
        val branch = get(Names.CDLIB_SANITIZED_BRANCH_NAME).lowercase()
        val app = get(Names.CDLIB_APP_NAME).lowercase()
        // 53 is maximum length for helm release name. 1 additional character for the dash between app and branch name
        val releaseNameLength = (53 - 1 - app.length).coerceAtLeast(0)
        if (releaseNameLength <= 0) {
            logger.warn {
                "The app name length is too long ($app). The branch name ($branch) is truncated from the helm release name."
            }
        }
        return if (branch.startsWith("renovate")) {
            "$app-${branch.takeLast(releaseNameLength)}".take(53)
        } else {
            "$app-${branch.take(releaseNameLength)}".take(53)
        }.dropLastWhile { !it.isLetterOrDigit() }
    }

    open fun createReleaseVersion(): String {
        return "${get(Names.CDLIB_APP_VERSION)}_${get(Names.CDLIB_BUILD_NUMBER)}_${get(Names.CDLIB_REVISION)}"
    }

    private fun createReleaseNameUnique(): String {
        val timestamp =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))

        return "${get(Names.CDLIB_RELEASE_NAME)}_$timestamp"

    }

    open fun createRevision(): String = get(Names.CDLIB_PM_GIT_ID).take(7)

    open fun createSanitizedBranchName(): String =
        get(Names.CDLIB_EFFECTIVE_BRANCH_NAME).replace("[^-A-Za-z0-9]".toRegex(), "-").lowercase()

    open fun createSemanticVersion(): String =
        buildString {
            append(get(Names.CDLIB_APP_VERSION))
            append("-")
            append(get(Names.CDLIB_EFFECTIVE_BRANCH_NAME))
            append(".")
            append(get(Names.CDLIB_BUILD_NUMBER))
            append(".")
            append(get(Names.CDLIB_REVISION))
        }

    open fun createTerraformPrefix(): String {
        val prefix = get(Names.CDLIB_PM_GIT_ORIGIN).split('/').last().split('.').first()
        return "$prefix-${get(Names.CDLIB_EFFECTIVE_BRANCH_NAME)}".replace("[^-A-Za-z0-9]".toRegex(), replacement = "-")
    }

    //
    // Internal implementation helpers
    //
    abstract fun createBuildNumber(): String

    abstract fun createCicdPlatform(): String

    abstract fun createPipelineUrl(): String

    private val gitRevision by lazy {
        val currentDir = namesConfigWithDefault.repoPath
        GitRepository.lastBranchCommit(currentDir)
    }
}
