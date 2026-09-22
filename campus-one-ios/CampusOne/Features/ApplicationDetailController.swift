import UIKit

/// 申请详情（对应 Vue ApplicationDetail）。基本信息 + 审批进度时间线。
final class ApplicationDetailController: UIViewController {
    private let id: Int
    private struct Application: Decodable {
        let title: String?
        let applicationNo: String?
        let status: String?
        let currentNode: String?
        let applicantName: String?
        let studentNo: String?
        let department: String?
        let className: String?
        let leaveType: String?
        let startTime: String?
        let endTime: String?
        let phone: String?
        let reason: String?
    }

    private var app: Application?
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
                let item: Application = try await APIClient.shared.request("GET", "applications/\(id)")
                await MainActor.run {
                    self.app = item
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

    private func buildUI(_ item: Application) {
        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        scroll.backgroundColor = .clear
        view.addSubview(scroll)
        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.topAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        let head = ArchiveHeader()
        head.titleLabel.text = item.title ?? "申请详情"
        head.subtitleLabel.text = item.applicationNo ?? ""
        let status = item.status ?? "PROCESSING"
        let statusStamp = StampView(statusText(status), style: statusStyle(status))
        var headItems: [UIView] = [head, statusStamp]
        let group = reviewGroup(item.currentNode)
        if !group.isEmpty {
            headItems.append(StampView(group + " · 负责审批", style: .line))
        }
        let headRow = UIStackView(arrangedSubviews: headItems)
        headRow.alignment = .center; headRow.spacing = 8

        // 基本信息
        let infoCard = ArchiveCard()
        let infoTitle = SectionHeader("基本信息")
        let meta = UIStackView()
        meta.axis = .vertical; meta.spacing = 8
        meta("申请人", item.applicantName ?? "-", into: meta)
        meta("学号", item.studentNo ?? "-", into: meta)
        meta("学院", item.department ?? "-", into: meta)
        meta("班级", item.className ?? "-", into: meta)
        meta("请假类型", item.leaveType ?? "-", into: meta)
        meta("开始时间", item.startTime ?? "-", into: meta)
        meta("结束时间", item.endTime ?? "-", into: meta)
        meta("联系电话", item.phone ?? "-", into: meta)
        let reason = UILabel(); reason.text = "请假原因："; reason.font = .systemFont(ofSize: 13); reason.textColor = Theme.ink500
        let reasonValue = UILabel(); reasonValue.text = item.reason ?? "-"; reasonValue.font = .systemFont(ofSize: 13); reasonValue.textColor = Theme.ink900; reasonValue.numberOfLines = 0
        let reasonRow = UIStackView(arrangedSubviews: [reason, reasonValue]); reasonRow.alignment = .top; reasonRow.spacing = 6
        meta.addArrangedSubview(reasonRow)
        let iStack = UIStackView(arrangedSubviews: [infoTitle, meta]); iStack.axis = .vertical; iStack.spacing = 12
        iStack.translatesAutoresizingMaskIntoConstraints = false
        infoCard.addSubview(iStack)

        // 审批进度
        let progCard = ArchiveCard()
        let progTitle = SectionHeader("审批进度")
        let steps = approvalSteps(status, item.currentNode)
        let progList = UIStackView(); progList.axis = .vertical; progList.spacing = 12
        for step in steps {
            let dot = UIView()
            dot.backgroundColor = step.done ? Theme.ink900 : Theme.ink300
            dot.layer.cornerRadius = 5
            dot.translatesAutoresizingMaskIntoConstraints = false
            dot.widthAnchor.constraint(equalToConstant: 10).isActive = true
            dot.heightAnchor.constraint(equalToConstant: 10).isActive = true
            let title = UILabel(); title.text = step.title
            title.font = .systemFont(ofSize: 13); title.textColor = step.done ? Theme.ink900 : (step.current ? Theme.ink700 : Theme.ink500)
            let sub = UILabel(); sub.text = step.sub; sub.font = .systemFont(ofSize: 11); sub.textColor = Theme.ink500
            let col = UIStackView(arrangedSubviews: [title, sub]); col.axis = .vertical; col.spacing = 2
            let row = UIStackView(arrangedSubviews: [dot, col]); row.alignment = .top; row.spacing = 8
            progList.addArrangedSubview(row)
        }
        let pStack = UIStackView(arrangedSubviews: [progTitle, progList]); pStack.axis = .vertical; pStack.spacing = 12
        pStack.translatesAutoresizingMaskIntoConstraints = false
        progCard.addSubview(pStack)

        let stack = UIStackView(arrangedSubviews: [headRow, infoCard, progCard])
        stack.axis = .vertical; stack.spacing = 18
        stack.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: scroll.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: scroll.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: scroll.topAnchor, constant: 20),
            stack.bottomAnchor.constraint(equalTo: scroll.bottomAnchor, constant: -20),
            stack.widthAnchor.constraint(equalTo: scroll.widthAnchor, constant: -32),
            iStack.leadingAnchor.constraint(equalTo: infoCard.leadingAnchor, constant: 16),
            iStack.trailingAnchor.constraint(equalTo: infoCard.trailingAnchor, constant: -16),
            iStack.topAnchor.constraint(equalTo: infoCard.topAnchor, constant: 16),
            iStack.bottomAnchor.constraint(equalTo: infoCard.bottomAnchor, constant: -16),
            pStack.leadingAnchor.constraint(equalTo: progCard.leadingAnchor, constant: 16),
            pStack.trailingAnchor.constraint(equalTo: progCard.trailingAnchor, constant: -16),
            pStack.topAnchor.constraint(equalTo: progCard.topAnchor, constant: 16),
            pStack.bottomAnchor.constraint(equalTo: progCard.bottomAnchor, constant: -16),
        ])
    }

    private func meta(_ label: String, _ value: String, into stack: UIStackView) {
        let l = UILabel(); l.text = label + "："; l.font = .systemFont(ofSize: 13); l.textColor = Theme.ink500
        l.setContentHuggingPriority(.required, for: .horizontal)
        let v = UILabel(); v.text = value; v.numberOfLines = 0; v.font = .systemFont(ofSize: 13); v.textColor = Theme.ink900
        let row = UIStackView(arrangedSubviews: [l, v]); row.alignment = .top; row.spacing = 6
        stack.addArrangedSubview(row)
    }

    /// 由后端写回的当前审批节点识别负责群体（教师/职工）
    private func reviewGroup(_ node: String?) -> String {
        let n = node ?? ""
        if n.contains("教师") { return "教师" }
        if n.contains("职工") { return "职工" }
        return ""
    }

    /// 审批进度步骤：根据状态与负责群体真实推进
    private func approvalSteps(_ status: String, _ node: String?) -> [(title: String, sub: String, done: Bool, current: Bool)] {
        let done = (status == "APPROVED" || status == "DONE")
        let rejected = (status == "REJECTED")
        let n = node ?? ""
        let group = reviewGroup(node)
        let reviewLabel = !n.isEmpty ? n : (group.isEmpty ? "负责审批" : group + "审批")
        return [
            ("提交申请", "系统登记", true, false),
            (reviewLabel, done ? "已通过" : (rejected ? "已驳回" : "处理中"), done, !done && !rejected),
            ("终审归档", done ? "流程已办结" : (rejected ? "已驳回" : "待审"), done, false),
        ]
    }

    private func statusText(_ status: String) -> String {
        switch status {
        case "APPROVED", "DONE": return "已办结"
        case "REJECTED": return "已驳回"
        case "WITHDRAWN": return "已撤回"
        default: return "审批中"
        }
    }
    private func statusStyle(_ status: String) -> StampView.Style {
        switch status {
        case "APPROVED", "DONE": return .solid
        default: return .line
        }
    }
}