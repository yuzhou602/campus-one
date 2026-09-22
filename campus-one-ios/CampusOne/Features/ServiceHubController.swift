import UIKit

/// 「服务」哈勃（对应 Vue 侧栏的校园功能）。按功能分组目录，点击进入对应页面。
final class ServiceHubController: UITableViewController {
    private struct Entry { let title: String; let no: String; let hint: String; let make: () -> UIViewController }
    private let groups: [(name: String, entries: [Entry])] = [
        (name: "校园服务", entries: [
            Entry(title: "校园事务", no: "01", hint: "发起事务申请", make: { ServiceController() }),
            Entry(title: "场地预约", no: "02", hint: "预约校园场地", make: { ReservationController() }),
            Entry(title: "校园报修", no: "03", hint: "提交报修工单", make: { RepairController() }),
        ]),
        (name: "资讯与协作", entries: [
            Entry(title: "校园活动", no: "04", hint: "活动报名", make: { ActivityController() }),
            Entry(title: "校园资讯", no: "05", hint: "查看通知公告", make: { NoticeController() }),
            Entry(title: "消息中心", no: "06", hint: "站内消息", make: { MessageController() }),
            Entry(title: "AI 校园助手", no: "07", hint: "智能问答", make: { AIController() }),
            Entry(title: "任务中心", no: "08", hint: "待办与进度", make: { TaskController() }),
        ]),
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "服务"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(ServiceHubCell.self, forCellReuseIdentifier: "cell")
        tableView.rowHeight = 64
        tableView.sectionHeaderHeight = 34
    }

    override func numberOfSections(in tableView: UITableView) -> Int { groups.count }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        groups[section].entries.count
    }
    override func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let h = SectionHeader(groups[section].name)
        h.translatesAutoresizingMaskIntoConstraints = false
        let wrap = UIView()
        wrap.backgroundColor = .clear
        wrap.addSubview(h)
        NSLayoutConstraint.activate([
            h.leadingAnchor.constraint(equalTo: wrap.leadingAnchor, constant: 20),
            h.trailingAnchor.constraint(equalTo: wrap.trailingAnchor, constant: -20),
            h.centerYAnchor.constraint(equalTo: wrap.centerYAnchor),
        ])
        return wrap
    }
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! ServiceHubCell
        cell.bind(groups[indexPath.section].entries[indexPath.row])
        return cell
    }
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let entry = groups[indexPath.section].entries[indexPath.row]
        navigationController?.pushViewController(entry.make(), animated: true)
    }
}

private final class ServiceHubCell: UITableViewCell {
    private let no = MonoLabel("")
    private let title = UILabel()
    private let hint = UILabel()
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        title.font = .systemFont(ofSize: 15, weight: .medium); title.textColor = Theme.ink900
        hint.font = .systemFont(ofSize: 12); hint.textColor = Theme.ink500
        let chevron = UIImageView(image: UIImage(systemName: "chevron.right")); chevron.tintColor = Theme.ink300
        [no, title, hint, chevron].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }
        contentView.addSubview(card)
        [no, title, hint, chevron].forEach { card.addSubview($0) }
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 5),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -5),
            no.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16), no.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: no.trailingAnchor, constant: 14), title.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            hint.leadingAnchor.constraint(equalTo: title.leadingAnchor), hint.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 3),
            chevron.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16), chevron.centerYAnchor.constraint(equalTo: card.centerYAnchor),
        ])
        accessoryType = .none
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
    func bind(_ e: ServiceHubController.Entry) {
        no.text = e.no
        title.text = e.title
        hint.text = e.hint
    }
}