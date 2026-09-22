import UIKit

/// 校园活动列表（对应 Vue ActivityList）。分类分段 + 活动卡（内含报名进度墨线），点击进详情。
final class ActivityController: UITableViewController {
    private struct Activity: Decodable {
        let id: Int
        let title: String?
        let categoryName: String?
        let startTime: String?
        let location: String?
        let organizer: String?
        let registered: Int?
        let capacity: Int?
    }

    private var activities: [Activity] = []
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let seg = UISegmentedControl(items: ["全部", "学术", "竞赛", "讲座", "社团", "体育", "志愿"])

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "校园活动"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(ActivityCell.self, forCellReuseIdentifier: "cell")

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
                let page: Page<Activity> = try await APIClient.shared.request("GET", "activities", query: ["page": "1", "pageSize": "50"])
                let items = page.records ?? []
                await MainActor.run {
                    self.activities = items
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

    @objc private func categoryChanged() {
        // 类别仅本地筛选展示（Vue 端仅存 activeCategory，未据此重新请求）
        tableView.reloadData()
    }

    private func filtered() -> [Activity] {
        guard seg.selectedSegmentIndex > 0 else { return activities }
        let mapCategory: [String?] = [nil, "学术", "竞赛", "讲座", "社团", "体育", "志愿"]
        let target = mapCategory[seg.selectedSegmentIndex]
        return activities.filter { ($0.categoryName?.contains(target ?? "") ?? false) }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        filtered().count + 1
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 150 : 180
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView(); host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.titleLabel.text = "校园活动"
            head.subtitleLabel.text = "翻阅本学期在案的活动记录。"
            head.markLabel.text = "ACT · 在档 \(filtered().count) 场"
            let segWrap = UIView()
            segWrap.addSubview(seg)
            seg.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                seg.leadingAnchor.constraint(equalTo: segWrap.leadingAnchor, constant: 16),
                seg.trailingAnchor.constraint(equalTo: segWrap.trailingAnchor, constant: -16),
                seg.topAnchor.constraint(equalTo: segWrap.topAnchor, constant: 6),
                seg.bottomAnchor.constraint(equalTo: segWrap.bottomAnchor, constant: -6),
            ])
            let col = UIStackView(arrangedSubviews: [head, segWrap])
            col.axis = .vertical; col.spacing = 14
            host.addSubview(col)
            col.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                col.leadingAnchor.constraint(equalTo: host.leadingAnchor, constant: 16),
                col.trailingAnchor.constraint(equalTo: host.trailingAnchor, constant: -16),
                col.topAnchor.constraint(equalTo: host.topAnchor, constant: 14),
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
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! ActivityCell
        cell.bind(no: String(format: "%02d", indexPath.row), item: filtered()[indexPath.row - 1])
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        let item = filtered()[indexPath.row - 1]
        navigationController?.pushViewController(ActivityDetailController(id: item.id), animated: true)
    }
}

/// 活动卡：标题 + 时间/地点 + 报名进度（墨线 UIProgressView）
private final class ActivityCell: UITableViewCell {
    private let noLabel = MonoLabel("", size: 9)
    private let titleLabel = UILabel()
    private let metaLabel = UILabel()
    private let orgLabel = UILabel()
    private let stamp = StampView("未分类", style: .line)
    private let progress = UIProgressView(progressViewStyle: .default)
    private let fillLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)

        titleLabel.font = .systemFont(ofSize: 15, weight: .semibold); titleLabel.textColor = Theme.ink900
        titleLabel.numberOfLines = 1
        metaLabel.font = .systemFont(ofSize: 12); metaLabel.textColor = Theme.ink500
        metaLabel.numberOfLines = 2
        orgLabel.font = .systemFont(ofSize: 12); orgLabel.textColor = Theme.ink500
        fillLabel.font = .mono(11); fillLabel.textColor = Theme.ink700

        progress.progressTintColor = Theme.ink900
        progress.trackTintColor = Theme.line
        progress.translatesAutoresizingMaskIntoConstraints = false

        card.addSubview(noLabel); card.addSubview(stamp); card.addSubview(titleLabel)
        card.addSubview(metaLabel); card.addSubview(orgLabel); card.addSubview(progress); card.addSubview(fillLabel)

        [noLabel, stamp, titleLabel, metaLabel, orgLabel, progress, fillLabel].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }

        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            noLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16), noLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            stamp.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16), stamp.centerYAnchor.constraint(equalTo: noLabel.centerYAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16), titleLabel.topAnchor.constraint(equalTo: noLabel.bottomAnchor, constant: 8),
            titleLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            metaLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor), metaLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
            metaLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8),
            orgLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor), orgLabel.topAnchor.constraint(equalTo: metaLabel.bottomAnchor, constant: 8),
            fillLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor), fillLabel.centerYAnchor.constraint(equalTo: orgLabel.centerYAnchor),
            progress.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor), progress.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
            progress.topAnchor.constraint(equalTo: orgLabel.bottomAnchor, constant: 10),
            progress.heightAnchor.constraint(equalToConstant: 4),
        ])
        accessoryType = .none
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(no: String, item: ActivityController.Activity) {
        noLabel.text = "NO·\(no)"
        titleLabel.text = item.title ?? "未命名活动"
        stamp.setText(item.categoryName ?? "未分类")
        stamp.setStyle(.line)
        stamp.isHidden = (item.categoryName ?? "").isEmpty
        var lines = [String]()
        if let t = item.startTime, !t.isEmpty { lines.append("时间：" + t) }
        if let l = item.location, !l.isEmpty { lines.append("地点：" + l) }
        metaLabel.text = lines.joined(separator: "\n")
        orgLabel.text = item.organizer ?? ""
        let reg = item.registered ?? 0
        let cap = item.capacity ?? 0
        let pct = cap > 0 ? Float(reg) / Float(cap) : 0
        progress.progress = min(max(pct, 0), 1)
        fillLabel.text = cap > 0 ? "\(reg)/\(cap) 人" : "\(reg) 人"
    }
}