package de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls


data class BundlerData(
    val bundles: List<BundlerDataBundle>,
    val transformationRules: List<BundlerDataTransformationRules>,
)

data class BundlerDataBundle(
    val bundleName: String,
    val licenseName: String,
    val licenseUrl: String,
)

data class BundlerDataTransformationRules(
    val bundleName: String,
    @JsonSetter(nulls = Nulls.SKIP)
    val licenseNamePattern: String = "",
    @JsonSetter(nulls = Nulls.SKIP)
    val licenseUrlPattern: String = "",
    @JsonSetter(nulls = Nulls.SKIP)
    val licenseFileContentPattern: String = "",
)
