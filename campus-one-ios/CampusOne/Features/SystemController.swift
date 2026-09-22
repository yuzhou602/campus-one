import UIKit

/// 系统管理（对应 Vue SystemManagement）。模块入口：用户管理可进入，其余「待建档」禁用。含运行信息。
final class SystemController: UIViewController {

    private let scrollView = UIScrollView()
    private let content = UIStackView()
    private var sysInfo = [UILabel]()          // 4 个信息值标签

    private struct Module { let title: String; let desc: String; let icon: String; let make: (() -> UIViewController)? }

    private let modules: [Module] = [
        Module(title: "用户管理", desc: "在案用户与权限", icon: "person.2", make: { SystemUsersController() }),
        Module(title: "角色管理", desc: "角色与授权关系", icon: "lock", make: nil),
        Module(title: "菜单管理", desc: "导航与功能菜单", icon: "line.3.horizontal", make: nil),
        Module(title: "系统配置", desc: "平台参数配置", icon: "gearshape", make: nil),
        Module(title: "字典管理", desc: "数据字典维护", icon: "book.closed", make: nil),
        Module(title: "操作日志", desc: "平台操作留痕", icon: "doc.text", make: nil),
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "系统管理"
        view.backgroundColor = Theme.paper
        buildUI()
        loadSysInfo()
    }

    private func buildUI() {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.alwaysBounceVertical = true
        view.addSubview(scrollView)
        content.axis = .vertical
        content.spacing = Theme.space
        content.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(content)
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            content.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor, constant: Theme.space),
            content.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor, constant: Theme.space),
            content.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor, constant: -Theme.space),
            content.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor, constant: -Theme.space),
            content.widthAnchor.constraint(equalTo: scrollView.widthAnchor, constant: -Theme.space * 2),
        ])

        let header = ArchiveHeader()
        header.titleLabel.text = "系统管理"
        header.subtitleLabel.text = "平台配置与运维信息。"
        header.markLabel.text = "SYSTEM · 总览"
        content.addArrangedSubview(header)

        // 运行信息卡
        let infoCard = ArchiveCard()
        let sec = SectionHeader("运行信息")
        let infoGrid = UIStackView(); infoGrid.axis = .vertical; infoGrid.spacing = 10
        let runs: [(String, Int)] = [("平台", 0), ("版本", 1), ("运行时", 2), ("系统", 3)]
        for (key, idx) in runs {
            let v = UILabel(); v.text = "—"; v.font = .mono(12); v.textColor = Theme.ink900
            v.lineBreakMode = .byTruncatingTail; v.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
            sysInfo.append(v)
            let k = UILabel(); k.text = key; k.font = .systemFont(ofSize: 10); k.textColor = Theme.ink300
            k.text = key.uppercased()
            let row = UIStackView(arrangedSubviews: [k, UIView(), v]); row.spacing = 8; row.alignment = .center
            row.backgroundColor = Theme.surface
            row.layoutMargins = UIEdgeInsets(top: 10, left: 12, bottom: 10, right: 12)
            row.isLayoutMarginsRelativeArrangement = true
            row.layer.borderWidth = 1; row.layer.borderColor = Theme.line.cgColor; row.layer.cornerRadius = 6
            infoGrid.addArrangedSubview(row)
        }
        infoCard.addSubview(sec); infoCard.addSubview(infoGrid)
        sec.translatesAutoresizingMaskIntoConstraints = false
        infoGrid.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            sec.topAnchor.constraint(equalTo: infoCard.topAnchor, constant: 16),
            sec.leadingAnchor.constraint(equalTo: infoCard.leadingAnchor, constant: 16),
            sec.trailingAnchor.constraint(equalTo: infoCard.trailingAnchor, constant: -16),
            infoGrid.topAnchor.constraint(equalTo: sec.bottomAnchor, constant: 12),
            infoGrid.leadingAnchor.constraint(equalTo: infoCard.leadingAnchor, constant: 16),
            infoGrid.trailingAnchor.constraint(equalTo: infoCard.trailingAnchor, constant: -16),
            infoGrid.bottomAnchor.constraint(equalTo: infoCard.bottomAnchor, constant: -16),
        ])
        content.addArrangedSubview(infoCard)

        // 模块网格（2 行 × 3 列）
        let grid = UIStackView(); grid.axis = .vertical; grid.spacing = 12
        var row = UIStackView(); row.axis = .horizontal; row.spacing = 12; row.distribution = .fillEqually
        var row2 = UIStackView(); row2.axis = .horizontal; row2.spacing = 12; row2.distribution = .fillEqually
        modules.enumerated().forEach { i, m in
            let box = moduleBox(m, no: String(format: "%02d", i + 1))
            (i < 3 ? row : row2).addArrangedSubview(box)
        }
        grid.addArrangedSubview(row); grid.addArrangedSubview(row2)
        content.addArrangedSubview(grid)
    }

    private func moduleBox(_ m: Module, no: String) -> UIView {
        let container = UIView()
        let card = ArchiveCard()
        card.layer.shadowOpacity = 0
        card.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(card)
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            card.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            card.topAnchor.constraint(equalTo: container.topAnchor),
            card.bottomAnchor.constraint(equalTo: container.bottomAnchor),
            container.heightAnchor.constraint(equalToConstant: 138),
        ])

        let icon = UIImageView(image: UIImage(systemName: m.icon))
        icon.tintColor = m.make == nil ? Theme.ink300 : Theme.ink700
        icon.contentMode = .scaleAspectFit
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.widthAnchor.constraint(equalToConstant: 24).isActive = true
        icon.heightAnchor.constraint(equalToConstant: 24).isActive = true

        // 用户管理可进入（记录可进入提示仅靠下方文字），其余「待建档」线章
        let flag = m.make == nil ? StampView("待建档", style: .line) : UIView()
        let topRow = UIStackView(arrangedSubviews: [icon, UIView(), flag])
        topRow.spacing = 8; topRow.alignment = .center

        let t = UILabel(); t.text = m.title; t.font = .systemFont(ofSize: 14, weight: .medium)
        t.textColor = m.make == nil ? Theme.ink500 : Theme.ink900
        let d = UILabel(); d.text = "前往管理 →"; d.font = .systemFont(ofSize: 11)
        d.textColor = m.make == nil ? Theme.ink300 : Theme.ink500

        let stack = UIStackView(arrangedSubviews: [topRow, t, d]); stack.axis = .vertical; stack.spacing = 8; stack.alignment = .leading
        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.isUserInteractionEnabled = false
        card.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 14),
            stack.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -14),
            stack.topAnchor.constraint(equalTo: card.topAnchor, constant: 14),
            stack.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -14),
        ])

        if let make = m.make {
            let btn = ButtonAction()
            btn.onTap = { [weak self] in
                self?.navigationController?.pushViewController(make(), animated: true)
            }
            btn.backgroundColor = .clear
            btn.frame = card.bounds
            btn.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            card.addSubview(btn)
        }
        return container
    }

    private func loadSysInfo() {
        Task {
            let info: SystemInfo? = try? await APIClient.shared.request("GET", "system/info")
            await MainActor.run {
                let values = [
                    info?.name ?? "—",
                    info?.version ?? "—",
                    info?.javaVersion ?? "—",
                    info?.osName ?? "—",
                ]
                zip(sysInfo, values).forEach { $0.text = $1 }
            }
        }
    }
}

private final class ButtonAction: UIButton {
    var onTap: (() -> Void)?
    override init(frame: CGRect) {
        super.init(frame: frame)
        addTarget(self, action: #selector(tapped), for: .touchUpInside)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
    @objc private func tapped() { onTap?() }
}