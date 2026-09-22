import UIKit

/// 消息中心（对应 Vue MessageCenter）。分类分段 + 消息列表（未读墨点）+ 全部已读。
final class MessageController: UITableViewController {
    private struct Message: Decodable {
        let id: Int
        let title: String?
        let content: String?
        let icon: String?
        let time: String?
        let read: Bool?
        // 兼容
        enum CodingKeys: String, CodingKey { case id, title, content, icon, time, read, isRead }
        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            id = try c.decode(Int.self, forKey: .id)
            title = try c.decodeIfPresent(String.self, forKey: .title)
            content = try c.decodeIfPresent(String.self, forKey: .content)
            icon = try c.decodeIfPresent(String.self, forKey: .icon)
            time = try c.decodeIfPresent(String.self, forKey: .time)
            read = try c.decodeIfPresent(Bool.self, forKey: .read) ?? (try c.decodeIfPresent(Bool.self, forKey: .isRead))
        }
    }

    private var messages: [Message] = []
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let seg = UISegmentedControl(items: ["全部", "审批", "预约", "报修", "活动", "系统"])
    private let markAll = UIButton(type: .system)

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "消息中心"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(MessageCell.self, forCellReuseIdentifier: "cell")

        markAll.setTitle("全部已读", for: .normal)
        markAll.setTitleColor(Theme.ink700, for: .normal)
        markAll.titleLabel?.font = .systemFont(ofSize: 13)
        markAll.addTarget(self, action: #selector(markAllRead), for: .touchUpInside)

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
                let data: [Message] = try await APIClient.shared.request("GET", "notifications/my", query: ["page": "1", "pageSize": "30"])
                await MainActor.run {
                    self.messages = data
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

    @objc private func markAllRead() {
        Task {
            do {
                try await APIClient.shared.requestVoid("PUT", "notifications/read-all")
                await MainActor.run { self.load() }
            } catch {
                await MainActor.run {
                    let a = UIAlertController(title: "操作失败", message: error.localizedDescription, preferredStyle: .alert)
                    a.addAction(UIAlertAction(title: "知道了", style: .default)); self.present(a, animated: true)
                }
            }
        }
    }

    override func numberOfSections(in tableView: UITableView) -> Int { 1 }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        messages.count + 1
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 140 : 88
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView(); host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.titleLabel.text = "消息中心"
            head.subtitleLabel.text = "查阅各类来函与事项通知。"
            head.markLabel.text = "MAIL · 收件 \(messages.count) 条"
            let segWrap = UIView()
            segWrap.addSubview(seg)
            seg.translatesAutoresizingMaskIntoConstraints = false
            let markWrap = UIView()
            markWrap.addSubview(markAll); markAll.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                seg.leadingAnchor.constraint(equalTo: segWrap.leadingAnchor, constant: 16),
                seg.trailingAnchor.constraint(lessThanOrEqualTo: markWrap.leadingAnchor, constant: -8),
                seg.centerYAnchor.constraint(equalTo: segWrap.centerYAnchor),
                markWrap.leadingAnchor.constraint(equalTo: seg.trailingAnchor, constant: 8),
                markWrap.trailingAnchor.constraint(equalTo: markAll.trailingAnchor, constant: 16),
                markAll.centerYAnchor.constraint(equalTo: markWrap.centerYAnchor),
                segWrap.heightAnchor.constraint(equalToConstant: 34),
                markWrap.heightAnchor.constraint(equalToConstant: 34),
            ])
            let bars = UIStackView(arrangedSubviews: [segWrap, markWrap])
            bars.axis = .horizontal; bars.alignment = .center
            let col = UIStackView(arrangedSubviews: [head, bars]); col.axis = .vertical; col.spacing = 12
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
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! MessageCell
        cell.bind(item: messages[indexPath.row - 1])
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        // Vue 端点击仅标已读，无跳转
        let msg = messages[indexPath.row - 1]
        let a = UIAlertController(title: msg.title ?? "消息", message: msg.content, preferredStyle: .alert)
        a.addAction(UIAlertAction(title: "知道了", style: .default)); present(a, animated: true)
    }
}

private final class MessageCell: UITableViewCell {
    private let iconView = UIImageView()
    private let titleLabel = UILabel()
    private let contentLabel = UILabel()
    private let timeLabel = UILabel()
    private let unreadDot = UIView()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)

        iconView.contentMode = .center
        iconView.tintColor = Theme.ink700
        iconView.backgroundColor = Theme.paper
        iconView.layer.borderWidth = 1
        iconView.layer.borderColor = Theme.line.cgColor
        iconView.layer.cornerRadius = 10

        titleLabel.font = .systemFont(ofSize: 14, weight: .medium); titleLabel.textColor = Theme.ink900
        contentLabel.font = .systemFont(ofSize: 12); contentLabel.textColor = Theme.ink500; contentLabel.numberOfLines = 1
        timeLabel.font = .systemFont(ofSize: 11); timeLabel.textColor = Theme.ink500
        unreadDot.backgroundColor = Theme.ink900; unreadDot.layer.cornerRadius = 4

        card.addSubview(iconView); card.addSubview(titleLabel); card.addSubview(contentLabel)
        card.addSubview(timeLabel); card.addSubview(unreadDot)
        [iconView, titleLabel, contentLabel, timeLabel, unreadDot].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            iconView.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 14), iconView.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 40), iconView.heightAnchor.constraint(equalToConstant: 40),
            titleLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 12),
            titleLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 14),
            timeLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            timeLabel.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: timeLabel.leadingAnchor, constant: -8),
            contentLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            contentLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -24),
            contentLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 6),
            unreadDot.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            unreadDot.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            unreadDot.widthAnchor.constraint(equalToConstant: 8), unreadDot.heightAnchor.constraint(equalToConstant: 8),
        ])
        accessoryType = .none
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(item: MessageController.Message) {
        iconView.image = UIImage(systemName: iconName(item.icon))
        titleLabel.text = item.title ?? "消息"
        contentLabel.text = item.content ?? ""
        timeLabel.text = item.time ?? ""
        let isRead = item.read ?? false
        unreadDot.isHidden = isRead
    }

    private func iconName(_ icon: String?) -> String {
        guard let icon, !icon.isEmpty else { return "envelope" }
        let map: [String: String] = [
            "approval": "checkmark.seal", "审批": "checkmark.seal",
            "reservation": "calendar", "预约": "calendar",
            "repair": "wrench", "报修": "wrench",
            "activity": "flag", "活动": "flag",
            "system": "gearshape", "系统": "gearshape",
        ]
        return map[icon.lowercased()] ?? "envelope"
    }
}