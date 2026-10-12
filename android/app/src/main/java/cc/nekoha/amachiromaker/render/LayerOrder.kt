package cc.nekoha.amachiromaker.render

/**
 * 绘制顺序修正。
 *
 * 原站数据（orderedLayers.json）里「チョーカー（项圈）」排在「服」**之后**，
 * 于是项圈会被画在高领衣服上面，看起来像"项圈系在衣服外面"；
 * 「+髪（追加发）」又排在「服」之前，长发会被衣服盖住。
 *
 * 这里按穿着逻辑给出修正顺序（数组元素是 orderedLayers 的下标，从底到顶）：
 *   项圈 → 压在衣服下面；追加发 → 垂在衣服上面。
 *
 * 注意：数据本身不动，只影响渲染顺序，所以与原站/网页版的预设依然完全兼容。
 */
object LayerOrder {

    /** 后发（後ろ髪）在 orderedLayers 里的下标。 */
    const val BACK_HAIR = 1

    /** 衣服（服）在 orderedLayers 里的下标。 */
    const val CLOTH = 16

    /** 25 个部位时的修正顺序；索引对应 orderedLayers 数组。 */
    private val FIXED_ORDER = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, // 背景 → 后发 → 脸 → 五官 → 兽耳/角/耳饰
        18, 19,                                            // 前发 → 侧发
        17,                                                // チョーカー：在头发之上（不被垂发切断）、在衣服之下
        16,                                                // 服
        15,                                                // +髪：追加发，垂在衣服上
        20, 21, 22, 23, 24                                  // 发饰 → 其他 → 袖 → 贴纸
    )

    /** 说明文案（用于界面提示）。 */
    const val ORDER_NOTE = "项圈改为压在头发之上、衣服之下；追加发垂在衣服之上"

    fun orderFor(size: Int): IntArray =
        if (size == FIXED_ORDER.size) FIXED_ORDER else IntArray(size) { it }
}
