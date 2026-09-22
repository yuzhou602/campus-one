import UIKit

/// 404（对应 Vue NotFound）。「该卷宗不在档」。
final class NotFoundController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        title = "未命中"

        let num = UILabel(); num.text = "404"
        num.font = UIFont.monospacedSystemFont(ofSize: 88, weight: .bold)
        num.textColor = Theme.ink300
        num.textAlignment = .center
        num.translatesAutoresizingMaskIntoConstraints = false

        let h = UILabel(); h.text = "该卷宗不在档"
        h.font = .systemFont(ofSize: 20, weight: .semibold); h.textColor = Theme.ink900
        h.textAlignment = .center

        let p = UILabel(); p.text = "你访问的页面不存在或已被移除"
        p.font = .systemFont(ofSize: 14); p.textColor = Theme.ink500
        p.textAlignment = .center

        let back = PrimaryButton("返回首页")
        back.addTarget(self, action: #selector(goHome), for: .touchUpInside)
        back.translatesAutoresizingMaskIntoConstraints = false
        back.widthAnchor.constraint(equalToConstant: 220).isActive = true

        let stack = UIStackView(arrangedSubviews: [num, h, p, back])
        stack.axis = .vertical
        stack.spacing = 16
        stack.alignment = .center
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            stack.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 28),
            stack.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -28),
        ])
    }

    @objc private func goHome() {
        if let nav = navigationController {
            nav.popToRootViewController(animated: true)
        }
    }
}