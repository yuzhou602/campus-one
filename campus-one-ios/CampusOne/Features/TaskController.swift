import UIKit

/// 任务中心（对应 Vue TaskCenter）。三栏档案：待审批 / 我的预约 / 我的报修。
final class TaskController: UIViewController {
    private struct TaskItem: Decodable {
        let id: Int?
        let applicationNo: String?
        let repairNo: String?
        let title: String?
        var displayText: String { applicationNo ?? repairNo ?? title ?? "任务" }
    }
    /// 兼容「裸数组」与「{records:[...]}」两种返回
    private struct FlexList<T: Decodable>: Decodable {
        let items: [T]
        enum Key: String, CodingKey { case records }
        init(from decoder: Decoder) throws {
            if let arr = try? decoder.singleValueContainer().decode([T].self) {
                items = arr
            } else {
                let obj = try decoder.container(keyedBy: Key.self)
                items = try obj.decodeIfPresent([T].self, forKey: .records) ?? []
            }
        }
    }
    private struct TaskData: Decodable {
        let pendingApprovals: [TaskItem]
        let myReservations: [TaskItem]
        let myRepairs: [TaskItem]
        enum CK: String, CodingKey { case pendingApprovals, myReservations, myRepairs }
        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CK.self)
            pendingApprovals = (try c.decodeIfPresent(FlexList<TaskItem>.self, forKey: .pendingApprovals))?.items ?? []
            myReservations = (try c.decodeIfPresent(FlexList<TaskItem>.self, forKey: .myReservations))?.items ?? []
            myRepairs = (try c.decodeIfPresent(FlexList<TaskItem>.self, forKey: .myRepairs))?.items ?? []
        }
    }

    private var data: TaskData?
    private let spinner = UIActivityIndicatorView(style: .medium)

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "任务中心"
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
                let data: TaskData = try await APIClient.shared.request("GET", "tasks/my")
                await MainActor.run {
                    self.data = data
                    self.spinner.stopAnimating()
                    self.buildUI()
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

    private func buildUI() {
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
        head.titleLabel.text = "任务中心"
        head.subtitleLabel.text = "统一管理待办的登记事项。"
        head.markLabel.text = "TODO · 统一在案"

        let stack = UIStackView(arrangedSubviews: [head,
                                                   panel("待审批", count: data?.pendingApprovals.count ?? 0, rows: data?.pendingApprovals ?? []),
                                                   panel("我的预约", count: data?.myReservations.count ?? 0, rows: data?.myReservations ?? []),
                                                   panel("我的报修", count: data?.myRepairs.count ?? 0, rows: data?.myRepairs ?? [])])
        stack.axis = .vertical; stack.spacing = 18
        stack.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: scroll.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: scroll.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: scroll.topAnchor, constant: 20),
            stack.bottomAnchor.constraint(equalTo: scroll.bottomAnchor, constant: -20),
            stack.widthAnchor.constraint(equalTo: scroll.widthAnchor, constant: -32),
        ])
    }

    private func panel(_ title: String, count: Int, rows: [TaskController.TaskItem]) -> UIView {
        let card = ArchiveCard()
        let sec = SectionHeader(title)
        let stamp = StampView("\(count)", style: .line)
        let titleRow = UIStackView(arrangedSubviews: [sec, UIView(), stamp])
        titleRow.alignment = .center; titleRow.spacing = 8

        let list = UIStackView()
        list.axis = .vertical; list.spacing = 8
        if rows.isEmpty {
            let empty = UILabel(); empty.text = "暂无待办"
            empty.font = .systemFont(ofSize: 12); empty.textColor = Theme.ink300
            list.addArrangedSubview(empty)
        } else {
            for (i, row) in rows.prefix(6).enumerated() {
                let l = UILabel()
                l.text = String(format: "%02d  %@", i + 1, row.displayText)
                l.font = .mono(12); l.textColor = Theme.ink700
                l.numberOfLines = 1
                list.addArrangedSubview(l)
            }
        }

        let stack = UIStackView(arrangedSubviews: [titleRow, list])
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
}