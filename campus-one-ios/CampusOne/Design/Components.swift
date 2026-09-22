import UIKit

/// 可复用「档案·纸墨」组件，与 Web 端全局类对应：
/// sheet / arch-sec / stamp / stamp-line / n-rec / primary button / input.

/// 档案卡（= .sheet）：纸面 + 墨线 + 极浅纸影
final class ArchiveCard: UIView {
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = Theme.surface
        layer.borderWidth = 1
        layer.borderColor = Theme.line.cgColor
        layer.cornerRadius = Theme.radius
        layer.shadowColor = Theme.ink900.cgColor
        layer.shadowOpacity = 0.06
        layer.shadowOffset = CGSize(width: 0, height: 3)
        layer.shadowRadius = 10
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
}

/// 页眉（标题 + 副题 + 右侧等宽登记号）
final class ArchiveHeader: UIView {
    let titleLabel = UILabel()
    let subtitleLabel = UILabel()
    let markLabel = UILabel() // = .arch-mark .n-rec，如 REC·2026-001

    init() {
        super.init(frame: .zero)
        titleLabel.font = .systemFont(ofSize: 22, weight: .semibold)
        titleLabel.textColor = Theme.ink900
        subtitleLabel.font = .systemFont(ofSize: 13)
        subtitleLabel.textColor = Theme.ink500
        markLabel.font = .mono(11)
        markLabel.textColor = Theme.ink300
        markLabel.textAlignment = .right

        [titleLabel, subtitleLabel, markLabel].forEach { addSubview($0) }
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        markLabel.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: topAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor),
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 2),
            subtitleLabel.leadingAnchor.constraint(equalTo: leadingAnchor),
            subtitleLabel.bottomAnchor.constraint(equalTo: bottomAnchor),
            markLabel.centerYAnchor.constraint(equalTo: centerYAnchor),
            markLabel.trailingAnchor.constraint(equalTo: trailingAnchor),
            markLabel.leadingAnchor.constraint(greaterThanOrEqualTo: titleLabel.trailingAnchor, constant: 12)
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
}

/// 小节标题 = .arch-sec（带前导墨点 + 后方墨线）
final class SectionHeader: UIView {
    let label = UILabel()
    init(_ text: String) {
        super.init(frame: .zero)
        let dot = UIView()
        dot.backgroundColor = Theme.ink900
        dot.layer.cornerRadius = 3
        dot.translatesAutoresizingMaskIntoConstraints = false

        label.text = text
        label.font = .systemFont(ofSize: 14, weight: .semibold)
        label.textColor = Theme.ink900

        let line = UIView()
        line.backgroundColor = Theme.line
        line.translatesAutoresizingMaskIntoConstraints = false

        [dot, label, line].forEach { addSubview($0) }
        NSLayoutConstraint.activate([
            dot.widthAnchor.constraint(equalToConstant: 6), dot.heightAnchor.constraint(equalToConstant: 6),
            dot.centerYAnchor.constraint(equalTo: centerYAnchor),
            dot.leadingAnchor.constraint(equalTo: leadingAnchor),
            label.leadingAnchor.constraint(equalTo: dot.trailingAnchor, constant: 8),
            label.centerYAnchor.constraint(equalTo: centerYAnchor),
            line.leadingAnchor.constraint(equalTo: label.trailingAnchor, constant: 10),
            line.trailingAnchor.constraint(equalTo: trailingAnchor),
            line.centerYAnchor.constraint(equalTo: centerYAnchor),
            line.heightAnchor.constraint(equalToConstant: 1),
        ])
        heightAnchor.constraint(equalToConstant: 26).isActive = true
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
}

/// 印章 = .stamp（实章 / 线章）
final class StampView: UIView {
    enum Style { case solid, line }
    private let label = UILabel()
    private var style: Style = .solid

    init(_ text: String, style: Style = .solid) {
        super.init(frame: .zero)
        self.style = style
        label.text = text
        label.font = .systemFont(ofSize: 11, weight: .semibold)
        label.textAlignment = .center
        self.translatesAutoresizingMaskIntoConstraints = false
        self.addSubview(label)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: topAnchor, constant: 2),
            label.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -2),
            label.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 8),
            label.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -8),
        ])
        applyStyle()
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    private func applyStyle() {
        switch style {
        case .solid:
            backgroundColor = Theme.stamp
            layer.borderColor = Theme.stamp.cgColor
            label.textColor = .white
        case .line:
            backgroundColor = .clear
            layer.borderColor = Theme.line.cgColor
            label.textColor = Theme.ink700
        }
        layer.borderWidth = 1
        layer.cornerRadius = 4
        // 轻微倾斜，似落印
        transform = CGAffineTransform(rotationAngle: -0.03)
    }
}

extension StampView {
    func setStyle(_ s: Style) { style = s; applyStyle() }
    func setText(_ t: String) { label.text = t }
    /// 读写文字（供 `stamp.text = ...` 与 `stamp.setText(...)` 两种用法统一编译）
    var text: String {
        get { label.text ?? "" }
        set { label.text = newValue }
    }
}

/// 等宽编号标签 = .n-rec
final class MonoLabel: UILabel {
    convenience init(_ text: String, size: CGFloat = 11) {
        self.init(frame: .zero)
        self.text = text
        self.font = .mono(size)
        self.textColor = Theme.ink300
    }
    @discardableResult
    func tint(_ c: UIColor) -> MonoLabel { textColor = c; return self }
}

/// 主按钮 = 墨黑、圆角、右箭头悬停轻移
final class PrimaryButton: UIButton {
    init(_ title: String) {
        super.init(frame: .zero)
        setTitle(title, for: .normal)
        setTitleColor(.white, for: .normal)
        titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        backgroundColor = Theme.ink900
        layer.cornerRadius = 8
        heightAnchor.constraint(equalToConstant: 50).isActive = true
        let arr = UIImageView(image: UIImage(systemName: "arrow.right"))
        arr.tintColor = .white
        arr.alpha = 0.7
        arr.translatesAutoresizingMaskIntoConstraints = false
        addSubview(arr)
        NSLayoutConstraint.activate([
            arr.centerYAnchor.constraint(equalTo: centerYAnchor),
            arr.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -18),
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
}

/// 墨线输入框：卡纸面 + 墨线，聚焦转浓墨
final class InkTextField: UITextField {
    let padding = UIEdgeInsets(top: 0, left: 14, bottom: 0, right: 14)
    init(_ placeholder: String) {
        super.init(frame: .zero)
        self.placeholder = placeholder
        backgroundColor = .white
        layer.borderWidth = 1
        layer.borderColor = Theme.line.cgColor
        layer.cornerRadius = 8
        layer.shadowColor = Theme.ink900.cgColor
        layer.shadowOffset = .zero
        layer.shadowOpacity = 0.06
        layer.shadowRadius = 3
        heightAnchor.constraint(equalToConstant: 50).isActive = true
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
    override func textRect(forBounds bounds: CGRect) -> CGRect { bounds.inset(by: padding) }
    override func editingRect(forBounds bounds: CGRect) -> CGRect { bounds.inset(by: padding) }
    override func becomeFirstResponder() -> Bool {
        let ok = super.becomeFirstResponder()
        if ok { layer.borderColor = Theme.ink900.cgColor }
        return ok
    }
    override func resignFirstResponder() -> Bool {
        let ok = super.resignFirstResponder()
        if ok { layer.borderColor = Theme.line.cgColor }
        return ok
    }
}

/// 纸张纹理底图（极淡点状肌理 = 页面直呼）
final class PaperView: UIView {
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = Theme.paper
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
}