import UIKit

/// 通知详情（对应 Vue NoticeDetail）。标题 + 发布者/时间 + 内容。
final class NoticeDetailController: UIViewController {
    private let id: Int
    private struct Notice: Decodable {
        let title: String?
        let important: Bool?
        let publisher: String?
        let publishTime: String?
        let content: String?
    }

    init(id: Int) {
        self.id = id
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        let spinner = UIActivityIndicatorView(style: .medium)
        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
        spinner.startAnimating()
        Task {
            do {
                let item: Notice = try await APIClient.shared.request("GET", "notifications/\(id)")
                await MainActor.run {
                    spinner.stopAnimating()
                    self.buildUI(item)
                }
            } catch {
                await MainActor.run {
                    spinner.stopAnimating()
                    let a = UIAlertController(title: "读取失败", message: error.localizedDescription, preferredStyle: .alert)
                    a.addAction(UIAlertAction(title: "知道了", style: .default)); self.present(a, animated: true)
                }
            }
        }
    }

    private func buildUI(_ item: Notice) {
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
        head.titleLabel.text = item.title ?? "通知详情"
        head.subtitleLabel.text = [item.publisher, item.publishTime].compactMap { $0 }.joined(separator: "  ·  ")

        let important = StampView("重要")
        important.isHidden = !(item.important ?? false)

        let headWrap = UIStackView(arrangedSubviews: [head, important])
        headWrap.axis = .vertical; headWrap.alignment = .leading; headWrap.spacing = 8

        let body = ArchiveCard()
        let content = UILabel()
        content.text = item.content ?? ""
        content.font = .systemFont(ofSize: 14); content.textColor = Theme.ink900
        content.numberOfLines = 0
        content.translatesAutoresizingMaskIntoConstraints = false
        body.addSubview(content)
        NSLayoutConstraint.activate([
            content.leadingAnchor.constraint(equalTo: body.leadingAnchor, constant: 18),
            content.trailingAnchor.constraint(equalTo: body.trailingAnchor, constant: -18),
            content.topAnchor.constraint(equalTo: body.topAnchor, constant: 18),
            content.bottomAnchor.constraint(equalTo: body.bottomAnchor, constant: -18),
        ])

        let stack = UIStackView(arrangedSubviews: [headWrap, body])
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
}