import UIKit

/// 我的报修（对应 Vue MyRepairs）。在案工单列表，按状态盖墨章，点击进详情。
final class MyRepairsController: UITableViewController {
    private struct Repair: Decodable {
        let id: Int
        let repairNo: String?
        let location: String?
        let category: String?
        let createdAt: String?
        let status: String?
    }

    private var repairs: [Repair] = []
    private let spinner = UIActivityIndicatorView(style: .medium)

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "我的报修"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(RepairListCell.self, forCellReuseIdentifier: "cell")

        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 40),
        ])
        load()
    }

    private func load() {
        spinner.startAnimating()
        Task {
            do {
                let page: Page<Repair> = try await APIClient.shared.request("GET", "repairs/my", query: ["page": "1", "pageSize": "20"])
                let items = page.records ?? []
                await MainActor.run {
                    self.repairs = items
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
        repairs.count + 1
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 96 : 92
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView(); host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.titleLabel.text = "我的报修"
            head.subtitleLabel.text = "已登记的报修工单在案记录。"
            head.markLabel.text = "REPAIR · 在档 \(repairs.count)"
            host.addSubview(head)
            head.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                head.leadingAnchor.constraint(equalTo: host.leadingAnchor, constant: 16),
                head.trailingAnchor.constraint(equalTo: host.trailingAnchor, constant: -16),
                head.topAnchor.constraint(equalTo: host.topAnchor, constant: 14),
            ])
            let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
            cell.backgroundColor = Theme.paper; cell.selectionStyle = .none
            cell.contentView.addSubview(host)
            host.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                host.leadingAnchor.constraint(equalTo: cell.contentView.leadingAnchor),
                host.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor),
                host.topAnchor.constraint(equalTo: cell.contentView.topAnchor),
                host.bottomAnchor.constraint(equalTo: cell.contentView.bottomAnchor),
            ])
            return cell
        }
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! RepairListCell
        cell.bind(item: repairs[indexPath.row - 1])
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        let item = repairs[indexPath.row - 1]
        navigationController?.pushViewController(RepairDetailController(id: item.id), animated: true)
    }
}

private final class RepairListCell: UITableViewCell {
    private let noLabel = MonoLabel("", size: 9)
    private let titleLabel = UILabel()
    private let metaLabel = UILabel()
    private let statusStamp = StampView("")

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)
        titleLabel.font = .systemFont(ofSize: 15, weight: .semibold); titleLabel.textColor = Theme.ink900
        metaLabel.font = .systemFont(ofSize: 11); metaLabel.textColor = Theme.ink500
        metaLabel.numberOfLines = 0
        [noLabel, titleLabel, metaLabel, statusStamp].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false; card.addSubview($0)
        }
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            noLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            noLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            titleLabel.leadingAnchor.constraint(equalTo: noLabel.trailingAnchor, constant: 10),
            titleLabel.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: statusStamp.leadingAnchor, constant: -8),
            statusStamp.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            statusStamp.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            metaLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            metaLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            metaLabel.topAnchor.constraint(equalTo: noLabel.bottomAnchor, constant: 8),
        ])
        accessoryType = .none
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(item: MyRepairsController.Repair) {
        noLabel.text = String(item.id)
        titleLabel.text = item.repairNo ?? "REPAIR·\(item.id)"
        var lines = [item.location ?? "", item.category ?? ""]
        if let t = item.createdAt { lines.append(t) }
        metaLabel.text = lines.joined(separator: "  ·  ")
        statusStamp.setText(statusText(item.status))
        statusStamp.setStyle(statusStyle(item.status))
    }

    private func statusText(_ status: String?) -> String {
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