import UIKit

/// 「校园档案 · 纸张与墨水」设计令牌（对齐 Web 端 index.css @theme）
enum Theme {
    // 颜色
    static let paper   = UIColor(hex: 0xF3EFE7) // 页面底：旧纸米黄
    static let surface = UIColor(hex: 0xFCFAF6) // 档案卡纸面
    static let ink900  = UIColor(hex: 0x221D18) // 主文字 / 标题
    static let ink700  = UIColor(hex: 0x4A443C) // 悬停 / 强次级
    static let ink500  = UIColor(hex: 0x6E665C) // 次级文字
    static let ink300  = UIColor(hex: 0xA49A8C) // 弱化 / 编号
    static let line    = UIColor(hex: 0xDDD4C5) // 墨线 / 缝线
    static let stamp   = UIColor(hex: 0x221D18) // 印章墨色

    // 间距
    static let space: CGFloat = 16
    static let radius: CGFloat = 10

    // 主题全局配置
    static func applyGlobal() {
        let nav = UINavigationBar.appearance()
        nav.barTintColor = paper
        nav.backgroundColor = paper
        nav.isTranslucent = false
        nav.titleTextAttributes = [.foregroundColor: ink900, .font: UIFont.systemFont(ofSize: 17, weight: .semibold)]
        nav.tintColor = ink900
        nav.shadowImage = UIImage()
        nav.setBackgroundImage(UIImage(), for: .default)

        let tab = UITabBar.appearance()
        tab.barTintColor = surface
        tab.tintColor = stamp
        tab.unselectedItemTintColor = ink300

        let seg = UISegmentedControl.appearance()
        seg.selectedSegmentTintColor = stamp
        seg.setTitleTextAttributes([.foregroundColor: UIColor.white], for: .selected)
        seg.setTitleTextAttributes([.foregroundColor: ink500], for: .normal)
    }

    /// 印章文字颜色（实章白字 / 线章墨字）
    enum Stamp {
        static let solidBg = stamp
        static let solidFg = UIColor.white
        static let lineFg  = ink700
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        let r = CGFloat((hex >> 16) & 0xFF) / 255
        let g = CGFloat((hex >> 8) & 0xFF) / 255
        let b = CGFloat(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b, alpha: 1)
    }
}

extension UIFont {
    /// 等宽编号字体（对齐 .n-rec）
    static func mono(_ size: CGFloat) -> UIFont {
        UIFont(name: "Menlo", size: size) ?? UIFont.monospacedSystemFont(ofSize: size, weight: .regular)
    }
}