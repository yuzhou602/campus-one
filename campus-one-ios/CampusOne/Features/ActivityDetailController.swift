import UIKit

/// 活动详情 + 报名（对应 Vue ActivityDetail）。详情信息 + 简介 + 立即报名。
final class ActivityDetailController: UIViewController {
    private let id: Int
    private struct Activity: Decodable {
        let id: Int
        let title: String?
        let activityNo: String?
        let startTime: String?
        let endTime: String?
        let location: String?
        let organizer: String?
        let enrolledCount: Int?
        let maxCount: Int?
        let description: String?
    }

    private var activity: Activity?
    private let spinner = UIActivityIndicatorView(style: .medium)
    private let registerButton = PrimaryButton("立即报名")

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
                let item: Activity = try await APIClient.shared.request("GET", "activities/\(id)")
                await MainActor.run {
                    self.activity = item
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

    private func buildUI(_ item: Activity) {
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
        head.titleLabel.text = item.title ?? "活动详情"
        head.subtitleLabel.text = item.activityNo ?? ""
        head.markLabel.text = registerStampText(item)

        // 旗帜占位
        let banner = ArchiveCard()
        banner.backgroundColor = Theme.surface
        let flag = UIImageView(image: UIImage(systemName: "flag.fill"))
        flag.tintColor = Theme.ink300
        flag.contentMode = .scaleAspectFit
        flag.translatesAutoresizingMaskIntoConstraints = false
        banner.addSubview(flag)
        banner.heightAnchor.constraint(equalToConstant: 120).isActive = true
        NSLayoutConstraint.activate([
            flag.centerXAnchor.constraint(equalTo: banner.centerXAnchor),
            flag.centerYAnchor.constraint(equalTo: banner.centerYAnchor),
            flag.widthAnchor.constraint(equalToConstant: 56),
            flag.heightAnchor.constraint(equalToConstant: 56),
        ])

        // 信息网格
        let infoTitle = SectionHeader("活动信息")
        let meta = UIStackView()
        meta.axis = .vertical; meta.spacing = 8
        meta("时间", [item.startTime, item.endTime].compactMap { $0 }.joined(separator: " - "), into: meta)
        meta("地点", item.location ?? "-", into: meta)
        meta("主办方", item.organizer ?? "-", into: meta)
        let enrolled = item.enrolledCount ?? 0
        let max = item.maxCount ?? 0
        meta("已报名", max > 0 ? "\(enrolled)/\(max) 人" : "\(enrolled) 人", into: meta)
        let infoStack = UIStackView(arrangedSubviews: [infoTitle, meta])
        infoStack.axis = .vertical; infoStack.spacing = 12

        let infoCard = ArchiveCard()
        infoStack.translatesAutoresizingMaskIntoConstraints = false
        infoCard.addSubview(infoStack)
        NSLayoutConstraint.activate([
            infoStack.leadingAnchor.constraint(equalTo: infoCard.leadingAnchor, constant: 16),
            infoStack.trailingAnchor.constraint(equalTo: infoCard.trailingAnchor, constant: -16),
            infoStack.topAnchor.constraint(equalTo: infoCard.topAnchor, constant: 16),
            infoStack.bottomAnchor.constraint(equalTo: infoCard.bottomAnchor, constant: -16),
        ])

        // 简介
        let descTitle = SectionHeader("活动介绍")
        let desc = UILabel()
        desc.text = item.description ?? "-"
        desc.font = .systemFont(ofSize: 14); desc.textColor = Theme.ink900
        desc.numberOfLines = 0

        registerButton.addTarget(self, action: #selector(register), for: .touchUpInside)

        let stack = UIStackView(arrangedSubviews: [head, banner, infoCard, descTitle, desc, registerButton])
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

    private func meta(_ label: String, _ value: String, into stack: UIStackView) {
        let l = UILabel(); l.text = label + "："; l.font = .systemFont(ofSize: 13); l.textColor = Theme.ink500
        l.setContentHuggingPriority(.required, for: .horizontal)
        let v = UILabel(); v.text = value; v.numberOfLines = 0; v.font = .systemFont(ofSize: 13); v.textColor = Theme.ink900
        let row = UIStackView(arrangedSubviews: [l, v]); row.alignment = .top; row.spacing = 6
        stack.addArrangedSubview(row)
    }

    private func registerStampText(_ item: Activity) -> String {
        let enrolled = item.enrolledCount ?? 0
        let max = item.maxCount ?? 0
        return max > 0 && enrolled >= max ? "已满" : "报名中"
    }

    @objc private func register() {
        guard let a = activity else { return }
        let enrolled = a.enrolledCount ?? 0
        let max = a.maxCount ?? 0
        if max > 0 && enrolled >= max {
            let al = UIAlertController(title: "提示", message: "本次活动已报满。", preferredStyle: .alert)
            al.addAction(UIAlertAction(title: "知道了", style: .default)); present(al, animated: true); return
        }
        registerButton.isEnabled = false
        spinner.startAnimating()
        Task {
            do {
                try await APIClient.shared.requestVoid("POST", "activities/\(id)/register")
                await MainActor.run {
                    self.spinner.stopAnimating(); self.registerButton.isEnabled = true
                    let al = UIAlertController(title: "报名成功", message: "已登记活动报名。", preferredStyle: .alert)
                    al.addAction(UIAlertAction(title: "知道了", style: .default)); self.present(al, animated: true)
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating(); self.registerButton.isEnabled = true
                    let al = UIAlertController(title: "报名失败", message: error.localizedDescription, preferredStyle: .alert)
                    al.addAction(UIAlertAction(title: "知道了", style: .default)); self.present(al, animated: true)
                }
            }
        }
    }
}