package eu.theblob42.idea.whichkey.config

data class SizeConfig(val min: Int, val max: Int)

data class WhichKeyConfig(
    val height: SizeConfig = SizeConfig(min = 4, max = 25),
    val width: SizeConfig = SizeConfig(min = 20, max = 50),
    val spacing: Int = 3,
    val padding: Pair<Int, Int> = Pair(0, 0)
//    layout = {
//        height = { min = 4, max = 25 }, -- min and max height of the columns
//        width = { min = 20, max = 50 }, -- min and max width of the columns
//        spacing = 3, -- spacing between columns
//        align = "left", -- align columns left, center or right
//    }
)
