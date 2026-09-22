import UIKit

/// 我的申请（对应 Vue MyApplications）。按状态分卷，审批中=线章 / 已通过=实章 / 已驳回=线章 / 已撤回=线章。
final class MyApplicationsController: UITableViewController {
    private struct Application: Decodable {
        let id: Int
        let applicationNo: String?
        let serviceName: String?
        let createdAt: String?
        let currentNode: String?
        let status: String?
    }

    private var applications: [Application] = []
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let seg = UISegmentedControl(items: ["全部", "审批中", "已通过", "已驳回", "已撤回"])
    private static let tabStatus = [nil, "PROCESSING", "APPROVED", "REJECTED", "WITHDRAWN"]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "我的申请"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(AppCell.self, forCellReuseIdentifier: "cell")

        seg.selectedSegmentIndex = 0
        seg.addTarget(self, action: #selector(tabChanged), for: .valueChanged)

        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 40),
        ])
        load()
    }

    @objc private func tabChanged() { load() }

    private func load() {
        spinner.startAnimating()
        var query = ["page": "1", "pageSize": "20"]
        if let status = Self.tabStatus[seg.selectedSegmentIndex] {
            query["status"] = status
        }
        Task {
            do {
                let page: Page<Application> = try await APIClient.shared.request("GET", "applications/my", query: query)
                let items = page.records ?? []
                await MainActor.run {
                    self.applications = items
                    self.tableView.reloadData()
                    self.spinner.stopAnimating()
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

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        applications.count + 1
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 120 : 92
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView(); host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.titleLabel.text = "我的申请"
            head.subtitleLabel.text = "在案的各项服务申请记录。"
            head.markLabel.text = "FORM · 在册 \(applications.count) 份"
            let segWrap = UIView()
            segWrap.addSubview(seg)
            seg.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                seg.leadingAnchor.constraint(equalTo: segWrap.leadingAnchor, constant: 16),
                seg.trailingAnchor.constraint(equalTo: segWrap.trailingAnchor, constant: -16),
                seg.topAnchor.constraint(equalTo: segWrap.topAnchor, constant: 6),
                seg.bottomAnchor.constraint(equalTo: segWrap.bottomAnchor, constant: -6),
            ])
            let col = UIStackView(arrangedSubviews: [head, segWrap]); col.axis = .vertical; col.spacing = 12
            host.addSubview(col); col.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                col.leadingAnchor.constraint(equalTo: host.leadingAnchor, constant: 16),
                col.trailingAnchor.constraint(equalTo: host.trailingAnchor, constant: -16),
                col.topAnchor.constraint(equalTo: host.topAnchor, constant: 14),
            ])
            let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
            cell.backgroundColor = Theme.paper; cell.selectionStyle = .none
            cell.contentView.addSubview(host); host.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                host.leadingAnchor.constraint(equalTo: cell.contentView.leadingAnchor),
                host.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor),
                host.topAnchor.constraint(equalTo: cell.contentView.topAnchor),
                host.bottomAnchor.constraint(equalTo: cell.contentView.bottomAnchor),
            ])
            return cell
        }
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! AppCell
        cell.bind(item: applications[indexPath.row - 1])
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        let item = applications[indexPath.row - 1]
        navigationController?.pushViewController(ApplicationDetailController(id: item.id), animated: true)
    }
}

private final class AppCell: UITableViewCell {
    private let noLabel = MonoLabel("", size: 9)
    private let titleLabel = UILabel()
    private let metaLabel = UILabel()
    private let nodeLabel = UILabel()
    private let stamp = StampView("")

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)
        titleLabel.font = .systemFont(ofSize: 15, weight: .semibold); titleLabel.textColor = Theme.ink900
        metaLabel.font = .mono(11); metaLabel.textColor = Theme.ink500
        nodeLabel.font = .systemFont(ofSize: 11); nodeLabel.textColor = Theme.ink500
        [noLabel, titleLabel, metaLabel, nodeLabel, stamp].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false; card.addSubview($0)
        }
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            noLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16), noLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            titleLabel.leadingAnchor.constraint(equalTo: noLabel.trailingAnchor, constant: 10),
            titleLabel.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: stamp.leadingAnchor, constant: -8),
            stamp.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            stamp.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            metaLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            metaLabel.topAnchor.constraint(equalTo: noLabel.bottomAnchor, constant: 10),
            nodeLabel.leadingAnchor.constraint(equalTo: metaLabel.leadingAnchor),
            nodeLabel.topAnchor.constraint(equalTo: metaLabel.bottomAnchor, constant: 4),
        ])
        accessoryType = .none
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(item: MyApplicationsController.Application) {
        noLabel.text = String(format: "%02d", item.id % 100)
        titleLabel.text = item.serviceName ?? "事务申请"
        metaLabel.text = item.applicationNo ?? "APPLY·\(item.id)"
        nodeLabel.text = [item.createdAt, item.currentNode].compactMap { $0 }.joined(separator: "  ·  ")
        let s = item.status ?? "PROCESSING"
        stamp.setText(Self.statusText(s))
        stamp.setStyle(Self.statusStyle(s))
    }

    static func statusText(_ status: String) -> String {
        switch status {
        case "APPROVED", "DONE": return "已办结"
        case "REJECTED": return "已驳回"
        case "WITHDRAWN": return "已撤回"
        default: return "审批中"
        }
    }
    static func statusStyle(_ status: String) -> StampView.Style {
        switch status {
        case "APPROVED", "DONE": return .solid
        default: return .line
        }
    }
}