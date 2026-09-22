import UIKit

/// 校园资讯列表（对应 Vue NoticeList）。分类分段 + 通知列表（未读墨点 / 重要墨章）。
final class NoticeController: UITableViewController {
    private struct Notice: Decodable {
        let id: Int
        let important: Bool?
        let title: String?
        let summary: String?
        let source: String?
        let time: String?
        let unread: Bool?
    }

    private var notices: [Notice] = []
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let seg = UISegmentedControl(items: ["全部", "学校", "学院", "班级", "系统"])

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "校园资讯"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(NoticeCell.self, forCellReuseIdentifier: "cell")

        seg.selectedSegmentIndex = 0
        seg.addTarget(self, action: #selector(categoryChanged), for: .valueChanged)

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
                let page: Page<Notice> = try await APIClient.shared.request("GET", "notices", query: ["page": "1", "pageSize": "50"])
                let items = page.records ?? []
                await MainActor.run {
                    self.notices = items
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

    @objc private func categoryChanged() { tableView.reloadData() }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        notices.count + 1
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 120 : 98
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView(); host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.titleLabel.text = "校园资讯"
            head.subtitleLabel.text = "翻阅在卷的校园动态。"
            head.markLabel.text = "BULLETIN · 在档 \(notices.count) 则"
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
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! NoticeCell
        cell.bind(no: String(format: "%02d", indexPath.row), item: notices[indexPath.row - 1])
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        let item = notices[indexPath.row - 1]
        navigationController?.pushViewController(NoticeDetailController(id: item.id), animated: true)
    }
}

private final class NoticeCell: UITableViewCell {
    private let noLabel = MonoLabel("", size: 9)
    private let titleLabel = UILabel()
    private let summaryLabel = UILabel()
    private let sourceLabel = UILabel()
    private let unreadDot = UIView()
    private let important = StampView("重要")

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)

        titleLabel.font = .systemFont(ofSize: 14, weight: .medium); titleLabel.textColor = Theme.ink900
        titleLabel.numberOfLines = 1
        summaryLabel.font = .systemFont(ofSize: 12); summaryLabel.textColor = Theme.ink500
        summaryLabel.numberOfLines = 1
        sourceLabel.font = .systemFont(ofSize: 11); sourceLabel.textColor = Theme.ink500

        unreadDot.backgroundColor = Theme.ink900
        unreadDot.layer.cornerRadius = 4

        card.addSubview(noLabel); card.addSubview(important); card.addSubview(titleLabel)
        card.addSubview(summaryLabel); card.addSubview(sourceLabel); card.addSubview(unreadDot)

        [noLabel, important, titleLabel, summaryLabel, sourceLabel, unreadDot].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }

        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            noLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16), noLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            unreadDot.leadingAnchor.constraint(equalTo: noLabel.trailingAnchor, constant: 10), unreadDot.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            unreadDot.widthAnchor.constraint(equalToConstant: 8), unreadDot.heightAnchor.constraint(equalToConstant: 8),
            titleLabel.leadingAnchor.constraint(equalTo: unreadDot.trailingAnchor, constant: 10),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: important.leadingAnchor, constant: -6),
            titleLabel.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            important.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            important.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            summaryLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            summaryLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            summaryLabel.topAnchor.constraint(equalTo: noLabel.bottomAnchor, constant: 8),
            sourceLabel.leadingAnchor.constraint(equalTo: summaryLabel.leadingAnchor),
            sourceLabel.topAnchor.constraint(equalTo: summaryLabel.bottomAnchor, constant: 6),
        ])
        accessoryType = .none
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(no: String, item: NoticeController.Notice) {
        noLabel.text = no
        titleLabel.text = item.title ?? "未命名通知"
        summaryLabel.text = item.summary ?? ""
        var meta = [item.source ?? ""]
        if let t = item.time, !t.isEmpty { meta.append(t) }
        sourceLabel.text = meta.joined(separator: "  ·  ")
        unreadDot.isHidden = !(item.unread ?? false)
        important.isHidden = !(item.important ?? false)
        important.setText("重要")
        important.setStyle(.solid)
    }
}