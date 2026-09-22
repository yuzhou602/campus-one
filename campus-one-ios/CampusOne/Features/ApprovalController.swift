import UIKit

/// 审批中心（对应 Vue ApprovalCenter）。三卷：待我审批 / 我已审批 / 我发起的。
final class ApprovalController: UITableViewController {

    private enum Tab: Int, CaseIterable { case pending = 0, processed, mine
        var title: String {
            switch self { case .pending: return "待我审批"; case .processed: return "我已审批"; case .mine: return "我发起的" }
        }
    }

    private let seg = UISegmentedControl(items: Tab.allCases.map { $0.title })
    private var tab: Tab = .pending

    private var pending: [ApprovalItem] = []
    private var processed: [ApprovalItem] = []
    private var mine: [ApprovalItem] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "审批中心"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.rowHeight = 92
        tableView.sectionHeaderHeight = 0
        tableView.sectionFooterHeight = 0
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
        tableView.register(ApprovalCell.self, forCellReuseIdentifier: "cell")

        seg.selectedSegmentIndex = Tab.pending.rawValue
        seg.addTarget(self, action: #selector(switched), for: .valueChanged)
        let segWrap = UIView()
        seg.translatesAutoresizingMaskIntoConstraints = false
        segWrap.addSubview(seg)
        NSLayoutConstraint.activate([
            seg.leadingAnchor.constraint(equalTo: segWrap.leadingAnchor, constant: 16),
            seg.trailingAnchor.constraint(equalTo: segWrap.trailingAnchor, constant: -16),
            seg.topAnchor.constraint(equalTo: segWrap.topAnchor, constant: 4),
            seg.bottomAnchor.constraint(equalTo: segWrap.bottomAnchor, constant: -4),
        ])
        tableView.tableHeaderView = segWrap
        segWrap.layoutIfNeeded()

        loadCurrent()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if let header = tableView.tableHeaderView {
            header.frame.size.height = 56
            tableView.tableHeaderView = header
        }
    }

    @objc private func switched(_ sender: UISegmentedControl) {
        tab = Tab(rawValue: sender.selectedSegmentIndex) ?? .pending
        loadCurrent()
    }

    private func loadCurrent() {
        let items: () -> [ApprovalItem] = {
            switch self.tab {
            case .pending: return self.pending
            case .processed: return self.processed
            case .mine: return self.mine
            }
        }
        if !items().isEmpty { tableView.reloadData(); return }
        switch tab {
        case .pending: refreshPending()
        case .processed: refreshProcessed()
        case .mine: refreshMine()
        }
    }

    private func refreshPending() {
        Task {
            do {
                let page: Page<ApprovalItem> = try await APIClient.shared.request("GET", "applications/approvals/pending",
                                                                                  query: ["page": "1", "pageSize": "20"])
                pending = page.records ?? []
                await MainActor.run { if self.tab == .pending { self.tableView.reloadData() } }
            } catch {
                await MainActor.run { self.showError(error) }
            }
        }
    }

    private func refreshProcessed() {
        Task {
            do {
                let page: Page<ApprovalItem> = try await APIClient.shared.request("GET", "applications/approvals/processed",
                                                                                  query: ["page": "1", "pageSize": "20"])
                processed = page.records ?? []
                await MainActor.run { if self.tab == .processed { self.tableView.reloadData() } }
            } catch {
                await MainActor.run { self.showError(error) }
            }
        }
    }

    private func refreshMine() {
        Task {
            do {
                let mine: [ApprovalItem] = try await APIClient.shared.request("GET", "applications/my")
                self.mine = mine
                await MainActor.run { if self.tab == .mine { self.tableView.reloadData() } }
            } catch {
                await MainActor.run { self.showError(error) }
            }
        }
    }

    // MARK: - Table

    override func numberOfSections(in tableView: UITableView) -> Int { 1 }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch tab {
        case .pending: return pending.count
        case .processed: return processed.count
        case .mine: return mine.count
        }
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! ApprovalCell
        let item: ApprovalItem
        switch tab {
        case .pending: item = pending[indexPath.row]
        case .processed: item = processed[indexPath.row]
        case .mine: item = mine[indexPath.row]
        }
        cell.bind(item, index: indexPath.row + 1,
                  showActions: tab == .pending,
                  onApprove: tab == .pending ? { [weak self] in self?.apply(indexPath, action: "APPROVE") } : nil,
                  onReject: tab == .pending ? { [weak self] in self?.apply(indexPath, action: "REJECT") } : nil)
        return cell
    }

    override func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        let empty = UILabel(); empty.textColor = Theme.ink300; empty.font = .systemFont(ofSize: 13)
        empty.textAlignment = .center; empty.numberOfLines = 0
        switch tab {
        case .pending: empty.text = "待审卷已清空，无在途申请"
        case .processed: empty.text = "还没有经你审批的卷宗"
        case .mine: empty.text = "尚未发起任何申请"
        }
        empty.isHidden = currentRowsCount() > 0
        return empty
    }
    override func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        currentRowsCount() > 0 ? 0 : 200
    }
    private func currentRowsCount() -> Int {
        switch tab { case .pending: return pending.count; case .processed: return processed.count; case .mine: return mine.count }
    }

    // MARK: - 通过 / 驳回

    private func apply(_ indexPath: IndexPath, action: String) {
        let item = pending[indexPath.row]
        Task {
            do {
                _ = try await APIClient.shared.request("POST", "applications/approvals/\(item.id)/\(action.lowercased())",
                                                       body: ApprovalActionRequest(action: action, comment: ""))
                await MainActor.run {
                    self.pending.remove(at: indexPath.row)
                    self.tableView.reloadData()
                }
            } catch {
                await MainActor.run { self.showError(error) }
            }
        }
    }

    private func showError(_ e: Error) {
        let alert = UIAlertController(title: "操作未完成", message: e.localizedDescription, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "知道了", style: .default))
        present(alert, animated: true)
    }
}

/// 审批行：编号 + 卷宗号 + 状态章 + 申请人/节点 + （待审时）通过/驳回
private final class ApprovalCell: UITableViewCell {
    private let no = MonoLabel("", size: 10)
    private let title = UILabel()
    private let stamp = StampView("", style: .line)
    private let sub = UILabel()
    private let approve = UIButton(type: .system)
    private let reject = UIButton(type: .system)

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        selectionStyle = .none
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false

        title.font = .systemFont(ofSize: 14, weight: .semibold); title.textColor = Theme.ink900
        title.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        sub.font = .systemFont(ofSize: 12); sub.textColor = Theme.ink500

        let titleRow = UIStackView(arrangedSubviews: [no, title, UIView(), stamp])
        titleRow.spacing = 8; titleRow.alignment = .center

        let subRow = UIStackView(arrangedSubviews: [sub, UIView(), actions()])
        subRow.spacing = 8; subRow.alignment = .center

        let stack = UIStackView(arrangedSubviews: [titleRow, subRow]); stack.axis = .vertical; stack.spacing = 8

        contentView.addSubview(card)
        card.addSubview(stack)
        stack.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 5),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -5),
            stack.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            stack.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -12),
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    private func actions() -> UIView {
        let wrap = UIView()
        approve.setTitle("通过", for: .normal)
        reject.setTitle("驳回", for: .normal)
        approve.titleLabel?.font = .systemFont(ofSize: 13, weight: .semibold)
        reject.titleLabel?.font = .systemFont(ofSize: 13)
        approve.backgroundColor = Theme.ink900
        approve.tintColor = .white
        reject.backgroundColor = Theme.paper
        reject.tintColor = Theme.ink700
        reject.layer.borderWidth = 1
        reject.layer.borderColor = Theme.line.cgColor
        [approve, reject].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
            $0.heightAnchor.constraint(equalToConstant: 30).isActive = true
            $0.widthAnchor.constraint(equalToConstant: 64).isActive = true
            $0.layer.cornerRadius = 6
            wrap.addSubview($0)
        }
        NSLayoutConstraint.activate([
            approve.leadingAnchor.constraint(equalTo: wrap.leadingAnchor),
            approve.topAnchor.constraint(equalTo: wrap.topAnchor), approve.bottomAnchor.constraint(equalTo: wrap.bottomAnchor),
            reject.leadingAnchor.constraint(equalTo: approve.trailingAnchor, constant: 8),
            reject.topAnchor.constraint(equalTo: wrap.topAnchor), reject.bottomAnchor.constraint(equalTo: wrap.bottomAnchor),
            wrap.heightAnchor.constraint(equalToConstant: 30),
        ])
        return wrap
    }

    func bind(_ item: ApprovalItem, index: Int, showActions: Bool, onApprove: (() -> Void)?, onReject: (() -> Void)?) {
        no.text = String(format: "%02d", index)
        title.text = item.applicationNo ?? "待审批申请"
        sub.text = (item.applicantName ?? "申请人 #\(item.applicantId ?? 0)") + " · " + (item.currentNode ?? "审批节点")
        if showActions {
            stamp.text = "待审"
            stamp.setStyle(.line)
        } else {
            stamp.text = statusText(item.status)
            stamp.setStyle(.line)
        }
        approve.isHidden = !showActions
        reject.isHidden = !showActions
        approve.removeTarget(nil, action: nil, for: .allEvents)
        reject.removeTarget(nil, action: nil, for: .allEvents)
        if let onApprove { approve.addTarget(self, action: #selector(tapApprove), for: .touchUpInside) }
        if let onReject { reject.addTarget(self, action: #selector(tapReject), for: .touchUpInside) }
        // 存储闭包
        objc_setAssociatedObject(self, &Key.approve, onApprove, .OBJC_ASSOCIATION_COPY)
        objc_setAssociatedObject(self, &Key.reject, onReject, .OBJC_ASSOCIATION_COPY)
    }
    @objc private func tapApprove() { (objc_getAssociatedObject(self, &Key.approve) as? () -> Void)?() }
    @objc private func tapReject() { (objc_getAssociatedObject(self, &Key.reject) as? () -> Void)?() }

    private func statusText(_ status: String?) -> String {
        let m: [String: String] = ["PENDING": "审批中", "APPROVED": "已办结", "REJECTED": "已驳回"]
        return m[status ?? ""] ?? (status ?? "未知")
    }
    private enum Key { static var approve = "approveClosure"; static var reject = "rejectClosure" }
}