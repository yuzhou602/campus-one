import UIKit

/// 登录页（对应 Vue LoginView）。档案·纸墨语言参照物。
final class LoginController: UIViewController {
    private let usernameField = InkTextField("用户名")
    private let passwordField = InkTextField("密码")
    private let button = PrimaryButton("登  录")

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        passwordField.isSecureTextEntry = true
        buildUI()
    }

    private func buildUI() {
        // 顶部档案标签
        let head = UIStackView()
        head.spacing = 10
        let dot = UIView(); dot.backgroundColor = Theme.ink900; dot.layer.cornerRadius = 4
        dot.translatesAutoresizingMaskIntoConstraints = false
        dot.widthAnchor.constraint(equalToConstant: 8).isActive = true
        dot.heightAnchor.constraint(equalToConstant: 8).isActive = true
        let cap = UILabel(); cap.text = "校园综合服务档案"; cap.font = .mono(11); cap.textColor = Theme.ink500
        let no = UILabel(); no.text = "REC · 2026-001"; no.font = .mono(11); no.textColor = Theme.ink300
        head.addArrangedSubview(dot); head.addArrangedSubview(cap); head.addArrangedSubview(UIView()); head.addArrangedSubview(no)
        head.alignment = .center

        // 登记卡
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false

        let ctitle = UILabel(); ctitle.text = "登记"; ctitle.font = .systemFont(ofSize: 28, weight: .semibold); ctitle.textColor = Theme.ink900
        let csub = UILabel(); csub.text = "请在此登记你的校园账号，继续校园日常。"; csub.font = .systemFont(ofSize: 14); csub.textColor = Theme.ink500

        button.addTarget(self, action: #selector(handleLogin), for: .touchUpInside)

        // 演示账号
        let demoTip = UILabel(); demoTip.text = "快捷档案 · 演示账号"; demoTip.font = .mono(10); demoTip.textColor = Theme.ink300
        let demoGrid = UIStackView(); demoGrid.axis = .vertical; demoGrid.spacing = 10
        let demos = [("管理员", "admin"), ("学生", "student01"), ("教师", "teacher01"), ("职工", "counselor01")]
        for (label, user) in demos {
            let b = demoButton(label, user)
            b.addTarget(self, action: #selector(fill(_:)), for: .touchUpInside)
            demoGrid.addArrangedSubview(b)
        }

        [cap, no, dot, ctitle, csub, button, demoTip, demoGrid].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }

        view.addSubview(head)
        view.addSubview(card)
        card.addSubview(ctitle)
        card.addSubview(csub)
        card.addSubview(usernameField)
        card.addSubview(passwordField)
        card.addSubview(button)
        card.addSubview(demoTip)
        card.addSubview(demoGrid)

        [usernameField, passwordField].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }

        NSLayoutConstraint.activate([
            head.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),
            head.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 28),
            head.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -28),

            card.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            card.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: 10),
            card.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 28),
            card.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -28),

            ctitle.topAnchor.constraint(equalTo: card.topAnchor, constant: 28),
            ctitle.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 28),
            csub.topAnchor.constraint(equalTo: ctitle.bottomAnchor, constant: 8),
            csub.leadingAnchor.constraint(equalTo: ctitle.leadingAnchor),
            csub.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -28),

            usernameField.topAnchor.constraint(equalTo: csub.bottomAnchor, constant: 24),
            usernameField.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 28),
            usernameField.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -28),
            passwordField.topAnchor.constraint(equalTo: usernameField.bottomAnchor, constant: 12),
            passwordField.leadingAnchor.constraint(equalTo: usernameField.leadingAnchor),
            passwordField.trailingAnchor.constraint(equalTo: usernameField.trailingAnchor),
            button.topAnchor.constraint(equalTo: passwordField.bottomAnchor, constant: 22),
            button.leadingAnchor.constraint(equalTo: usernameField.leadingAnchor),
            button.trailingAnchor.constraint(equalTo: usernameField.trailingAnchor),

            demoTip.topAnchor.constraint(equalTo: button.bottomAnchor, constant: 24),
            demoTip.leadingAnchor.constraint(equalTo: usernameField.leadingAnchor),
            demoGrid.topAnchor.constraint(equalTo: demoTip.bottomAnchor, constant: 10),
            demoGrid.leadingAnchor.constraint(equalTo: usernameField.leadingAnchor),
            demoGrid.trailingAnchor.constraint(equalTo: usernameField.trailingAnchor),
            demoGrid.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -24),
        ])
    }

    private func demoButton(_ label: String, _ user: String) -> UIButton {
        let b = UIButton(type: .system)
        b.backgroundColor = Theme.surface
        b.layer.borderWidth = 1
        b.layer.borderColor = Theme.line.cgColor
        b.layer.cornerRadius = 8
        b.heightAnchor.constraint(equalToConstant: 52).isActive = true
        var cfg = UIButton.Configuration.plain()
        cfg.contentInsets = NSDirectionalEdgeInsets(top: 0, leading: 14, bottom: 0, trailing: 14)
        let dot = UIView(); dot.backgroundColor = Theme.ink900; dot.layer.cornerRadius = 2.5
        dot.widthAnchor.constraint(equalToConstant: 5).isActive = true
        dot.heightAnchor.constraint(equalToConstant: 5).isActive = true
        let l = UILabel(); l.text = label; l.font = .systemFont(ofSize: 14, weight: .medium); l.textColor = Theme.ink900
        let u = UILabel(); u.text = "\(user) · 123456"; u.font = .mono(10); u.textColor = Theme.ink300
        let row = UIStackView(arrangedSubviews: [dot, l, UIView(), u]); row.spacing = 10; row.alignment = .center
        row.isUserInteractionEnabled = false
        b.addSubview(row); row.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([row.topAnchor.constraint(equalTo: b.topAnchor),
                                     row.bottomAnchor.constraint(equalTo: b.bottomAnchor),
                                     row.leadingAnchor.constraint(equalTo: b.leadingAnchor, constant: 14),
                                     row.trailingAnchor.constraint(equalTo: b.trailingAnchor, constant: -14)])
        b.accessibilityLabel = "\(label) \(user)"
        return b
    }

    @objc private func fill(_ sender: UIButton) {
        // 从 accessibilityLabel 取值：格式 "标签 user"
        guard let text = sender.accessibilityLabel else { return }
        let parts = text.split(separator: " ")
        if let user = parts.last {
            usernameField.text = String(user)
            passwordField.text = "123456"
        }
    }

    @objc private func handleLogin() {
        guard let username = usernameField.text, !username.isEmpty,
              let password = passwordField.text, !password.isEmpty else { return }
        button.isEnabled = false
        let req = LoginRequest(username: username, password: password)
        Task {
            do {
                let res: LoginResponse = try await APIClient.shared.request("POST", "auth/login", body: req)
                let user = res.userInfo
                AuthStore.shared.save(token: res.accessToken, refreshToken: res.refreshToken,
                                      username: user.username, realName: user.realName, role: user.role)
                await MainActor.run { AppRouter.shared.switchToMain() }
            } catch {
                await MainActor.run {
                    self.button.isEnabled = true
                    self.showError(error)
                }
            }
        }
    }

    private func showError(_ e: Error) {
        let alert = UIAlertController(title: "登记未完成", message: e.localizedDescription, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "知道了", style: .default))
        present(alert, animated: true)
    }
}
