import UIKit

/// 校园办事大厅（对应 Vue ServiceCenter）。目录式枚举可发起的事务，点击进入申请登记页。
final class ServiceController: UITableViewController {
    private struct ServiceItem: Decodable {
        let id: Int
        let name: String?
        let description: String?
        let audience: String?
        let duration: String?
        let approvalFlow: String?
    }

    private var services: [ServiceItem] = []
    private let spinner = UIActivityIndicatorView(style: .medium)

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "办事大厅"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(ServiceCell.self, forCellReuseIdentifier: "cell")

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
                let items: [ServiceItem] = try await APIClient.shared.request("GET", "services")
                await MainActor.run {
                    self.services = items
                    self.tableView.reloadData()
                    self.spinner.stopAnimating()
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating()
                    let alert = UIAlertController(title: "读取失败", message: error.localizedDescription, preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "知道了", style: .default))
                    self.present(alert, animated: true)
                }
            }
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        services.count + 1 // +1 页眉行
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 96 : 150
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView()
            host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.translatesAutoresizingMaskIntoConstraints = false
            head.titleLabel.text = "校园办事大厅"
            head.subtitleLabel.text = "为每一件校园小事，登记一张凭证。"
            head.markLabel.text = "SVC · 目录 \(services.count) 项"
            host.addSubview(head)
            NSLayoutConstraint.activate([
                head.leadingAnchor.constraint(equalTo: host.leadingAnchor, constant: 16),
                head.trailingAnchor.constraint(equalTo: host.trailingAnchor, constant: -16),
                head.topAnchor.constraint(equalTo: host.topAnchor, constant: 14),
            ])
            let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
            cell.backgroundColor = Theme.paper
            cell.selectionStyle = .none
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
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! ServiceCell
        let item = services[indexPath.row - 1]
        cell.bind(no: String(format: "%02d", indexPath.row), item: item)
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        let item = services[indexPath.row - 1]
        navigationController?.pushViewController(ServiceApplyController(serviceId: item.id), animated: true)
    }
}

/// 事务卡：编号 + 名称 + 描述 + 适用/耗时 + 审批链
private final class ServiceCell: UITableViewCell {
    private let noLabel = MonoLabel("", size: 10)
    private let nameLabel = UILabel()
    private let descLabel = UILabel()
    private let metaLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear

        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)

        nameLabel.font = .systemFont(ofSize: 15, weight: .semibold)
        nameLabel.textColor = Theme.ink900
        descLabel.font = .systemFont(ofSize: 12)
        descLabel.textColor = Theme.ink500
        descLabel.numberOfLines = 2
        metaLabel.font = .mono(10)
        metaLabel.textColor = Theme.ink300
        metaLabel.numberOfLines = 0

        [noLabel, nameLabel, descLabel, metaLabel].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
            card.addSubview($0)
        }

        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),

            noLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            noLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 14),
            nameLabel.leadingAnchor.constraint(equalTo: noLabel.trailingAnchor, constant: 12),
            nameLabel.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            nameLabel.trailingAnchor.constraint(lessThanOrEqualTo: card.trailingAnchor, constant: -16),

            descLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            descLabel.topAnchor.constraint(equalTo: noLabel.bottomAnchor, constant: 8),
            descLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            metaLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            metaLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            metaLabel.topAnchor.constraint(equalTo: descLabel.bottomAnchor, constant: 8),
            metaLabel.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -12),
        ])
        accessoryType = .none
        let chevron = UIImageView(image: UIImage(systemName: "chevron.right"))
        chevron.tintColor = Theme.ink300
        chevron.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(chevron)
        NSLayoutConstraint.activate([
            chevron.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            chevron.centerYAnchor.constraint(equalTo: card.centerYAnchor),
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(no: String, item: ServiceController.ServiceItem) {
        noLabel.text = no
        nameLabel.text = item.name ?? "未命名事务"
        descLabel.text = item.description ?? ""
        let audience = item.audience ?? "全体"
        let duration = item.duration ?? "—"
        let flow = item.approvalFlow ?? "—"
        metaLabel.text = "适用 · \(audience)      约 \(duration)\n审批 · \(flow)"
    }
}