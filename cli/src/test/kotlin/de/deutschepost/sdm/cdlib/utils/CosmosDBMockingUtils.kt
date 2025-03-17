package de.deutschepost.sdm.cdlib.utils

import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardClient
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardClientPROD
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject


fun mockCosmosDBClient() {
    mockkConstructor(CosmosDashboardClientPROD::class)
    every { anyConstructed<CosmosDashboardClientPROD>().getVersionInfo(any()) } returns CosmosDashboardClient.VersionInfo()
}

fun mockChangeHandler(changeHandler: ChangeHandler) {
    mockkObject(changeHandler)
    every { changeHandler.readVersionInfo() } returns CosmosDashboardClient.VersionInfo()
}


fun mockCosmosDBVersionInfo() {
    mockkConstructor(CosmosDashboardClient.VersionInfo::class)
    every { anyConstructed<CosmosDashboardClient.VersionInfo>().isSupported } returns true
    every { anyConstructed<CosmosDashboardClient.VersionInfo>().isLatest } returns true
    every { anyConstructed<CosmosDashboardClient.VersionInfo>().id } returns "0.0"
    every { anyConstructed<CosmosDashboardClient.VersionInfo>().version } returns "0.0"
}
