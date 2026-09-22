import UIKit

/// 报修详情（对应 Vue RepairDetail）。故障信息 + 报修位置 + 联系人 + 时间线。
final class RepairDetailController: UIViewController {
    private let id: Int
    private struct Repair: Decodable {
        let id: Int?
        let repairNo: String?
        let title: String?
        let status: String?
        let description: String?
        let location: String?
        let contactName: String?
        let contactPhone: String?
        let createdAt: String?
        let category: String?
        let urgency: String?
    }

    private var repair: Repair?
    private let spinner = UIActivityIndicatorView(style: .medium)

    init(id: Int) {
        self.id = id
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
        load()
    }

    private func load() {
        spinner.startAnimating()
        Task {
            do {
                let item: Repair = try await APIClient.shared.request("GET", "repairs/\(id)")
                await MainActor.run {
                    self.repair = item
                    self.spinner.stopAnimating()
                    self.buildUI(item)
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating()
                    let a = UIAlertController(title: "读取失败", message: error.localizedDescription, preferredStyle: .alert)
                    a.addAction(UIAlertAction(title: "知道了", style: .default)); self.present(a, animated: true)
                }
            }
        }
    }

    private func buildUI(_ item: Repair) {
        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scroll)
        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.topAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        let head = ArchiveHeader()
        head.titleLabel.text = item.title ?? "报修工单"
        head.subtitleLabel.text = item.repairNo ?? ""
        head.markLabel.text = statusStampText(item.status)

        let info = ArchiveCard()
        let infoTitle = SectionHeader("故障信息")
        let metaStack = UIStackView()
        metaStack.axis = .vertical
        metaStack.spacing = 8
        metaStack.alignment = .fill
        meta("故障描述", item.description ?? "-", into: metaStack)
        meta("报修位置", item.location ?? "-", into: metaStack)
        meta("联系人", [item.contactName, item.contactPhone].compactMap { $0 }.joined(separator: " "), into: metaStack)
        meta("提交时间", item.createdAt ?? "-", into: metaStack)
        let catText = [item.category, item.urgency].compactMap { $0 }.joined(separator: " · ")
        meta("AI 分类", catText.isEmpty ? "-" : catText, into: metaStack)
        let iStack = UIStackView(arrangedSubviews: [infoTitle, metaStack])
        iStack.axis = .vertical; iStack.spacing = 12
        iStack.translatesAutoresizingMaskIntoConstraints = false
        info.addSubview(iStack)

        let tl = timeline()

        let stack = UIStackView(arrangedSubviews: [head, info, StampView(statusStampText(item.status), style: statusStyle(item.status)), tl])
        stack.axis = .vertical
        stack.spacing = 18
        stack.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: scroll.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: scroll.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: scroll.topAnchor, constant: 20),
            stack.bottomAnchor.constraint(equalTo: scroll.bottomAnchor, constant: -20),
            stack.widthAnchor.constraint(equalTo: scroll.widthAnchor, constant: -32),
            iStack.leadingAnchor.constraint(equalTo: info.leadingAnchor, constant: 16),
            iStack.trailingAnchor.constraint(equalTo: info.trailingAnchor, constant: -16),
            iStack.topAnchor.constraint(equalTo: info.topAnchor, constant: 16),
            iStack.bottomAnchor.constraint(equalTo: info.bottomAnchor, constant: -16),
        ])
    }

    private func meta(_ label: String, _ value: String, into stack: UIStackView) {
        let l = UILabel(); l.text = label + "："; l.font = .systemFont(ofSize: 13); l.textColor = Theme.ink500
        l.setContentHuggingPriority(.required, for: .horizontal)
        let v = UILabel(); v.text = value; v.font = .systemFont(ofSize: 13); v.textColor = Theme.ink900
        v.numberOfLines = 0
        let row = UIStackView(arrangedSubviews: [l, v])
        row.alignment = .top; row.spacing = 6
        stack.addArrangedSubview(row)
    }

    private func timeline() -> UIView {
        let card = ArchiveCard()
        let title = SectionHeader("工单时间线")
        let steps: [(String, String)] = [("用户提交报修工单", itemTime(0)), ("AI 分类与派单", ""), ("维修人员接单", ""), ("处理中", "")]
        let s = UIStackView()
        s.axis = .vertical; s.spacing = 12
        for (i, step) in steps.enumerated() {
            let dot = UIView()
            dot.backgroundColor = (i == 0) ? Theme.ink900 : Theme.ink300
            dot.layer.cornerRadius = 5
            dot.translatesAutoresizingMaskIntoConstraints = false
            dot.widthAnchor.constraint(equalToConstant: 10).isActive = true
            dot.heightAnchor.constraint(equalToConstant: 10).isActive = true
            let text = UILabel(); text.text = step.0; text.font = .systemFont(ofSize: 13); text.textColor = (i == 0) ? Theme.ink900 : Theme.ink700
            let time = UILabel(); time.text = step.1; time.font = .mono(10); time.textColor = Theme.ink300
            let row = UIStackView(arrangedSubviews: [dot, text, UIView(), time])
            row.alignment = .center; row.spacing = 8
            s.addArrangedSubview(row)
        }
        let stack = UIStackView(arrangedSubviews: [title, s])
        stack.axis = .vertical; stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -16),
        ])
        return card
    }

    private func itemTime(_ n: Int) -> String {
        repair?.createdAt ?? ""
    }

    private func statusStampText(_ status: String?) -> String {
        switch status?.uppercased() {
        case "DONE", "FINISHED", "COMPLETED": return "已办结"
        case "REJECTED": return "已驳回"
        default: return "处理中"
        }
    }
    private func statusStyle(_ status: String?) -> StampView.Style {
        switch status?.uppercased() {
        case "DONE", "FINISHED", "COMPLETED": return .solid
        default: return .line
        }
    }
}