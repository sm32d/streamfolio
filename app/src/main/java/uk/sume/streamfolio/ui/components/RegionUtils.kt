package uk.sume.streamfolio.ui.components

data class RegionInfo(
    val code: String,
    val name: String,
    val flag: String
)

object RegionUtils {
    val ALL_REGIONS: List<RegionInfo> = listOf(
        RegionInfo("AU", "Australia", "🇦🇺"),
        RegionInfo("BR", "Brazil", "🇧🇷"),
        RegionInfo("CA", "Canada", "🇨🇦"),
        RegionInfo("FR", "France", "🇫🇷"),
        RegionInfo("DE", "Germany", "🇩🇪"),
        RegionInfo("HK", "Hong Kong", "🇭🇰"),
        RegionInfo("IN", "India", "🇮🇳"),
        RegionInfo("JP", "Japan", "🇯🇵"),
        RegionInfo("SG", "Singapore", "🇸🇬"),
        RegionInfo("ZA", "South Africa", "🇿🇦"),
        RegionInfo("KR", "South Korea", "🇰🇷"),
        RegionInfo("AE", "United Arab Emirates", "🇦🇪"),
        RegionInfo("GB", "United Kingdom", "🇬🇧"),
        RegionInfo("US", "United States", "🇺🇸")
    ).sortedBy { it.name }

    val REGIONS_MAP: Map<String, String> = ALL_REGIONS.associate { it.code to it.name }

    fun getRegionInfo(code: String): RegionInfo {
        val upper = code.uppercase()
        return ALL_REGIONS.find { it.code == upper }
            ?: RegionInfo("US", "United States", "🇺🇸")
    }

    fun getFormattedRegionName(code: String): String {
        val info = getRegionInfo(code)
        return "${info.flag} ${info.name}"
    }
}
