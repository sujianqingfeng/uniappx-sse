以下是一个针对 **Japandi 风格网页/UI 设计** 的简洁实用**设计规范**（Design System Guidelines），你可以直接拿来用在 Figma / 项目文档 / AI 生成提示里。基于 Japandi 核心原则（极简 + 禅意 + 温暖功能主义），并结合你之前提到的那个例子（干净、大量留白、原木质感调）。

### 1. 整体哲学与原则
- **Less but better**：每个元素都要有存在的理由，去掉 80% 的装饰
- **Ma（間） + Hygge**：大量负空间 + 温暖舒适感并存
- **Wabi-sabi 微观不完美**：轻微的不对称、自然纹理优于完美对称
- **平静 > 刺激**：避免任何高饱和色、动态效果、弹窗、闪烁

### 2. 配色规范（Color Palette）
主色调：极度克制的中性 + 自然大地色

| 角色          | 色名             | Hex       | RGB            | 使用场景建议                  |
|---------------|------------------|-----------|----------------|-------------------------------|
| Background    | Warm Off-White   | #F5F2ED   | 245, 242, 237  | 主背景、卡片底                |
| Surface       | Soft Beige       | #EDE6DE   | 237, 230, 222  | 次级背景、输入框              |
| Text Primary  | Deep Charcoal    | #2D2A25   | 45, 42, 37     | 正文、标题                    |
| Text Secondary| Warm Gray        | #6B665C   | 107, 102, 92   | 辅助文字、placeholder         |
| Accent / Border| Muted Taupe     | #8A8175   | 138, 129, 117  | 细线、分隔线、hover 轻微强调  |
| Dark Accent   | Almost Black     | #1A1714   | 26, 23, 20     | 极少数按钮、图标（慎用）      |
| Nature Hint   | Olive / Sage     | #8A9980   | 138, 153, 128  | 极少量点缀（图标、tag）       |

- 饱和度建议：几乎所有颜色 ≤ 15–20%
- 对比度：保持 AA/AAA 标准（文本对比 ≥ 4.5:1）
- 禁止：任何 #FF0000 系、荧光色、纯黑 #000000、纯白 #FFFFFF

### 3. 排版规范（Typography）
- **字体选择**（推荐免费/商用可用的组合）：
  - 主字体（Heading + Body）：serif 或 semi-serif，带一点人文温度
    - Playfair Display（标题）
    - Georgia / Merriweather / Source Serif 4（正文）
    - 备选：Noto Serif JP + Inter（中日英混排友好）
  - 等宽/代码：JetBrains Mono 或 IBM Plex Mono（极淡灰）
- **字号与行高**（mobile-first 优先）：
  - H1 / Hero：3.5–4.5rem / line-height 1.1–1.15
  - H2：2.25–3rem / 1.15–1.2
  - H3：1.5–1.875rem / 1.25
  - Body：1.125–1.25rem / 1.6–1.75
  - Small / Caption：0.875–1rem / 1.5
- 字重：Light 300 / Regular 400 / Medium 500（几乎不用 Bold 700+）
- 对齐：左对齐为主，极少数居中（标题、hero）
- 字距：tracking +0.02–0.05em（标题稍松）

### 4. 间距与网格（Spacing & Grid）
- **8pt 网格系统**（推荐倍数：4、8、12、16、24、32、48、64、96、128）
- **常用间距**：
  - 元素内边距（padding）：16 / 24 / 32px
  - 组件间距：32 / 48 / 64px
  - 区块间距（section）：80–160px（越大越 Japandi）
  - 页面边距（margin to viewport）：min 5vw 或 64px
- **留白至上**：单个页面内容区宽度建议 ≤ 75–85ch（约 900–1100px）

### 5. 组件规范（Core Components）
- **按钮**：无填充 / 极细 1px 边框 / 圆角 0–4px / hover 只变色不放大
- **卡片**：无阴影 或 极淡内阴影（blur 4–8px） / 圆角 0–8px
- **输入框**：下划线式 或 极淡背景 + 1px 底边 / focus 只变底色
- **导航**：极简横排 / hamburger 只在 mobile / 文字链接为主
- **图标**：线条图标（stroke 1.5–2px） / 极简几何形状 / 无填充
- **图片**：原木/石材/亚麻/纸张纹理叠加（opacity 5–15%） / 圆角很小或直角

### 6. 质感与细节（Texture & Micro-details）
- 轻微噪点 / 纸张纹理（overlay 3–8%）
- 木纹 / 水泥 / 亚麻布料感（极低 opacity 背景）
- 过渡动画：全部 ease-in-out 300–500ms，禁止 bounce / elastic
- 圆角：全局 0–8px（直角更日式，微圆更北欧）

### 7. 禁止清单（Never Do）
- 渐变（除非极淡的单色到透明）
- 霓虹/高光/玻璃态
- 过多动画/视差
- 彩色图标/emoji 在正文
- 三种以上字体
- 居右布局（极少用）

你可以把这段规范直接复制到你的 Notion / Figma description / Claude / Midjourney / GLM 的提示词里，加上“严格遵循以下 Japandi 设计规范生成网页”就能得到很一致的效果。

---

## Playground 首页（落地规范）

目标：把功能测试页做成「留白充足、信息清晰、低刺激」的工具面板，优先可读性与可操作性。

### 信息架构
- Hero：标题 + 一句话说明
- Card 01「配置」：Channel ID / Scopes / Nonce / Only LINE App
- Card 02「操作」：登录/登出（主操作）+ profile/credential/refresh/verify（次操作）
- Card 03「Logs」：结果输出 + 复制/清空

### 色彩 Token（与页面一致）
- Background：`#F5F2ED`
- Surface：`#EDE6DE`（叠加透明度以保持层次）
- Text Primary：`#2D2A25`
- Text Secondary：`#6B665C`
- Border/Divider：`#8A8175`（使用低透明度）

### 组件落地要点
- 按钮：默认「无填充 + 细边框」；主按钮仅做轻微底色区分（不使用高饱和 primary）
- 卡片：无阴影；用细边框 + 轻微对比区分层级
- 表单：统一高度、圆角小、背景偏浅；辅助说明用 Secondary 色
