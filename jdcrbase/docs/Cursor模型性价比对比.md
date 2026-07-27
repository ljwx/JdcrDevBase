# Cursor 模型选型速查表

> 数据来源：[Models & Pricing](https://cursor.com/docs/models-and-pricing.md)、[CursorBench 3.2](https://cursor.com/cursorbench)  
> 整理日期：2026-07-27｜价格：$/百万 tokens｜Cost/task：按实际 token 计价  
> 性价比≈ = Score% ÷ Cost/task（仅相对比较）  
> \* Grok：训练时误含早期 Cursor 代码快照，榜分可能偏高，幅度不明

---

## 目录

1. [任务速查](#1-任务速查一眼看)
2. [日常推荐档（精简）](#2-日常推荐档精简)
3. [多维度排行](#3-多维度排行)
4. [完整 CursorBench 榜（按分数）](#4-完整-cursorbench-榜按分数)
5. [决策树与总结](#5-决策树与总结)
6. [附录：单价与 Fast](#6-附录单价与-fast)

---

## 1. 任务速查（一眼看）

| 你要做的事 | 用哪个 | 为什么 |
|-----------|--------|--------|
| 日常改代码 / 批量小任务 | **Composer 2.5** | 最便宜、大额度池 |
| 多文件 / 要更聪明 | **Grok 4.5 High（关 Fast）** | 接近 Opus 5 High，约 $1.5/任务 |
| 架构 / 难关 / 反复失败 | **Opus 5 High~Max** 或 **Sol Max** | 顶格能力 |
| 急、不差钱 | 对应模型开 Fast | 更快更贵 |
| 极省、接受更弱 | Luna / Terra Medium | 约 $0.4–$0.6/任务 |
| 不建议当默认 | Gemini 3.6、Kimi K2.7 | 不如 Composer 划算 |

**额度池：** Composer / Grok → **Cursor Models（更宽裕）**；其余 → **Other Models**  
**发送前确认：** 标签不要带 `Fast`（Grok Fast 输出约贵 3 倍）

---

## 2. 日常推荐档（精简）

每个家族只留「最该用」的一档，列尽量少。按**推荐优先级**排列。

| 优先级 | 模型 | Score | Cost/task | Steps | 额度池 | 何时用 |
|-------:|------|------:|----------:|------:|--------|--------|
| 1 | **Composer 2.5** | 56.1% | $0.44 | 33 | Cursor | 默认干活 |
| 2 | **Grok 4.5 High\*** | 66.7% | $1.51 | 33 | Cursor | 默认不够聪明时升级 |
| 3 | Grok 4.5 Medium\* | 65.4% | $1.54 | 34 | Cursor | 想稍保守一点 |
| 4 | Opus 5 High | 66.7% | $3.91 | 48 | Other | 关键难关 |
| 5 | GPT-5.6 Sol Max | 67.2% | $5.69 | 48 | Other | OpenAI 顶格攻坚 |
| 6 | Opus 5 Max | 70.0% | $8.23 | 78 | Other | 几乎最强，很贵 |
| 7 | GPT-5.6 Terra High | 54.2% | $0.89 | 23 | Other | Other 池里想省一点 |
| 8 | GPT-5.6 Luna High | 56.8% | $0.82 | 40 | Other | 分数接近 Composer，仍不如它划算 |
| — | Gemini 3.6 Flash High | 53.5% | $1.56 | 64 | Other | 一般不推荐默认 |
| — | Kimi K2.7 Code | 49.7% | $1.43 | 58 | Other | 不推荐默认 |
| — | Fable 5 High | 66.5% | $8.77 | 48 | Other | 强但贵，按需 |

---

## 3. 多维度排行

以下均取 CursorBench 有成绩的配置；表只保留选型关键列。

### 3.1 按能力（Score ↓）— Top 12

| # | 模型 | Score | Cost/task | Steps |
|--:|------|------:|----------:|------:|
| 1 | Fable 5 Max | 70.5% | $17.32 | 72 |
| 2 | Opus 5 Max | 70.0% | $8.23 | 78 |
| 3 | Opus 5 Extra High | 69.3% | $7.35 | 72 |
| 4 | Fable 5 Extra High | 68.4% | $11.73 | 56 |
| 5 | GPT-5.6 Sol Max | 67.2% | $5.69 | 48 |
| 6 | Opus 5 High | 66.7% | $3.91 | 48 |
| 7 | **Grok 4.5 High\*** | **66.7%** | **$1.51** | **33** |
| 8 | Fable 5 High | 66.5% | $8.77 | 48 |
| 9 | Grok 4.5 Medium\* | 65.4% | $1.54 | 34 |
| 10 | Fable 5 Medium | 65.2% | $6.80 | 41 |
| 11 | GPT-5.6 Terra Max | 64.9% | $2.89 | 47 |
| 12 | GPT-5.6 Sol Extra High | 64.5% | $3.88 | 38 |

> 同分段里看 Cost：Grok High 明显更划算。

### 3.2 按成本（Cost/task ↑）— Top 12 最便宜

| # | 模型 | Cost/task | Score | Steps |
|--:|------|----------:|------:|------:|
| 1 | GPT-5.6 Luna Low | $0.16 | 37.6% | 17 |
| 2 | GPT-5.6 Luna Medium | $0.39 | 47.7% | 28 |
| 3 | **Composer 2.5** | **$0.44** | **56.1%** | **33** |
| 4 | GPT-5.6 Terra Low | $0.53 | 46.9% | 19 |
| 5 | GPT-5.6 Terra Medium | $0.61 | 50.3% | 20 |
| 6 | GPT-5.6 Luna High | $0.82 | 56.8% | 40 |
| 7 | GPT-5.6 Terra High | $0.89 | 54.2% | 23 |
| 8 | GPT-5.5 Low | $0.98 | 46.6% | 20 |
| 9 | GPT-5.6 Sol Low | $1.01 | 52.6% | 19 |
| 10 | Gemini 3.6 Flash Low | $1.13 | 47.4% | 50 |
| 11 | GPT-5.6 Luna Extra High | $1.14 | 57.7% | 48 |
| 12 | GLM 5.2 High | $1.19 | 51.5% | 49 |

> 比 Composer 更便宜的，分数通常明显更弱；**$0.44 档的 Composer 是「又便宜又够用」的甜点**。

### 3.3 按性价比（Score÷Cost ↓）— Top 12

| # | 模型 | 性价比≈ | Score | Cost/task | Steps |
|--:|------|--------:|------:|----------:|------:|
| 1 | GPT-5.6 Luna Low | 235.0 | 37.6% | $0.16 | 17 |
| 2 | **Composer 2.5** | **127.5** | **56.1%** | **$0.44** | **33** |
| 3 | GPT-5.6 Luna Medium | 122.3 | 47.7% | $0.39 | 28 |
| 4 | GPT-5.6 Terra Low | 88.5 | 46.9% | $0.53 | 19 |
| 5 | GPT-5.6 Terra Medium | 82.5 | 50.3% | $0.61 | 20 |
| 6 | GPT-5.6 Luna High | 69.3 | 56.8% | $0.82 | 40 |
| 7 | GPT-5.6 Terra High | 60.9 | 54.2% | $0.89 | 23 |
| 8 | Grok 4.5 Low\* | 52.0 | 63.5% | $1.22 | 31 |
| 9 | GPT-5.6 Sol Low | 52.1 | 52.6% | $1.01 | 19 |
| 10 | GPT-5.6 Luna Extra High | 50.6 | 57.7% | $1.14 | 48 |
| 11 | GPT-5.5 Low | 47.6 | 46.6% | $0.98 | 20 |
| 12 | **Grok 4.5 High\*** | **44.2** | **66.7%** | **$1.51** | **33** |

> Luna Low 数字好看是因为太便宜，能力弱；**实用性价比看 Composer（够用）和 Grok High（够强）**。

### 3.4 按步数（Steps ↑）— Top 12 最省步

| # | 模型 | Steps | Score | Cost/task |
|--:|------|------:|------:|----------:|
| 1 | GPT-5.6 Luna Low | 17 | 37.6% | $0.16 |
| 2 | GPT-5.6 Sol Low | 19 | 52.6% | $1.01 |
| 3 | GPT-5.6 Terra Low | 19 | 46.9% | $0.53 |
| 4 | GPT-5.6 Terra Medium | 20 | 50.3% | $0.61 |
| 5 | GPT-5.5 Low | 20 | 46.6% | $0.98 |
| 6 | GPT-5.6 Terra High | 23 | 54.2% | $0.89 |
| 7 | GPT-5.5 Medium | 25 | 53.8% | $1.51 |
| 8 | GPT-5.6 Sol Medium | 27 | 60.0% | $1.95 |
| 9 | Opus 4.8 Low | 27 | 53.1% | $2.02 |
| 10 | GPT-5.5 High | 28 | 58.4% | $2.05 |
| 11 | GPT-5.6 Luna Medium | 28 | 47.7% | $0.39 |
| 12 | GPT-5.6 Terra Extra High | 29 | 59.2% | $1.44 |

**对比：推荐档的步数**

| 模型 | Steps | 说明 |
|------|------:|------|
| Composer 2.5 | 33 | 日常够稳 |
| Grok 4.5 High\* | 33 | 强且不啰嗦 |
| Opus 5 High | 48 | 更强，步数更多 |
| Gemini 3.6 Flash High | 64 | 偏爱空转 |
| Sonnet 5 Max | 86 | 最爱空转之一 |

---

## 4. 完整 CursorBench 榜（按分数）

全量 50 条，列已精简。需要查某配置时用本节。

| # | 模型 | Score | Cost/task | Tokens | Steps | 性价比≈ | 池 |
|--:|------|------:|----------:|-------:|------:|--------:|----|
| 1 | Fable 5 Max | 70.5% | $17.32 | 103525 | 72 | 4.1 | Other |
| 2 | Opus 5 Max | 70.0% | $8.23 | 61838 | 78 | 8.5 | Other |
| 3 | Opus 5 Extra High | 69.3% | $7.35 | 54239 | 72 | 9.4 | Other |
| 4 | Fable 5 Extra High | 68.4% | $11.73 | 64971 | 56 | 5.8 | Other |
| 5 | GPT-5.6 Sol Max | 67.2% | $5.69 | 28320 | 48 | 11.8 | Other |
| 6 | Opus 5 High | 66.7% | $3.91 | 27932 | 48 | 17.1 | Other |
| 7 | Grok 4.5 High\* | 66.7% | $1.51 | 19521 | 33 | 44.2 | Cursor |
| 8 | Fable 5 High | 66.5% | $8.77 | 43747 | 48 | 7.6 | Other |
| 9 | Grok 4.5 Medium\* | 65.4% | $1.54 | 18914 | 34 | 42.5 | Cursor |
| 10 | Fable 5 Medium | 65.2% | $6.80 | 30366 | 41 | 9.6 | Other |
| 11 | GPT-5.6 Terra Max | 64.9% | $2.89 | 32969 | 47 | 22.5 | Other |
| 12 | GPT-5.6 Sol Extra High | 64.5% | $3.88 | 19699 | 38 | 16.6 | Other |
| 13 | Opus 5 Medium | 64.3% | $3.29 | 23612 | 44 | 19.5 | Other |
| 14 | Grok 4.5 Low\* | 63.5% | $1.22 | 15841 | 31 | 52.0 | Cursor |
| 15 | GPT-5.6 Sol High | 63.5% | $2.79 | 13867 | 32 | 22.8 | Other |
| 16 | Opus 5 Low | 62.8% | $2.55 | 18529 | 37 | 24.6 | Other |
| 17 | Opus 4.8 Max | 62.3% | $5.77 | 71411 | 44 | 10.8 | Other |
| 18 | Fable 5 Low | 62.1% | $4.46 | 18182 | 31 | 13.9 | Other |
| 19 | Sonnet 5 Max | 61.5% | $6.45 | 92882 | 86 | 9.5 | Other |
| 20 | GPT-5.6 Luna Max | 61.1% | $1.97 | 87973 | 61 | 31.0 | Other |
| 21 | GPT-5.6 Sol Medium | 60.0% | $1.95 | 9747 | 27 | 30.8 | Other |
| 22 | Opus 4.8 Extra High | 59.4% | $4.50 | 51121 | 40 | 13.2 | Other |
| 23 | GPT-5.6 Terra Extra High | 59.2% | $1.44 | 16089 | 29 | 41.1 | Other |
| 24 | Sonnet 5 Extra High | 58.7% | $4.16 | 52871 | 67 | 14.1 | Other |
| 25 | GPT-5.5 High | 58.4% | $2.05 | 12183 | 28 | 28.5 | Other |
| 26 | GPT-5.5 Extra High | 58.4% | $2.85 | 17534 | 32 | 20.5 | Other |
| 27 | Opus 4.8 High | 58.0% | $3.15 | 33548 | 33 | 18.4 | Other |
| 28 | GPT-5.6 Luna Extra High | 57.7% | $1.14 | 22480 | 48 | 50.6 | Other |
| 29 | Sonnet 5 High | 56.9% | $3.19 | 39483 | 57 | 17.8 | Other |
| 30 | GPT-5.6 Luna High | 56.8% | $0.82 | 15141 | 40 | 69.3 | Other |
| 31 | Opus 4.8 Medium | 56.1% | $2.81 | 28384 | 32 | 20.0 | Other |
| 32 | Composer 2.5 | 56.1% | $0.44 | 14286 | 33 | 127.5 | Cursor |
| 33 | GLM 5.2 Max | 55.0% | $1.76 | 35946 | 58 | 31.3 | Other |
| 34 | GPT-5.6 Terra High | 54.2% | $0.89 | 9468 | 23 | 60.9 | Other |
| 35 | GPT-5.5 Medium | 53.8% | $1.51 | 8522 | 25 | 35.6 | Other |
| 36 | Gemini 3.6 Flash High | 53.5% | $1.56 | 30436 | 64 | 34.3 | Other |
| 37 | Opus 4.8 Low | 53.1% | $2.02 | 19624 | 27 | 26.3 | Other |
| 38 | GPT-5.6 Sol Low | 52.6% | $1.01 | 5104 | 19 | 52.1 | Other |
| 39 | Sonnet 5 Medium | 52.4% | $2.16 | 26200 | 46 | 24.3 | Other |
| 40 | GLM 5.2 High | 51.5% | $1.19 | 21829 | 49 | 43.3 | Other |
| 41 | Gemini 3.6 Flash Medium | 51.2% | $1.48 | 28511 | 62 | 34.6 | Other |
| 42 | GPT-5.6 Terra Medium | 50.3% | $0.61 | 6222 | 20 | 82.5 | Other |
| 43 | Kimi K2.7 Code | 49.7% | $1.43 | 31247 | 58 | 34.8 | Other |
| 44 | Gemini 3.5 Flash | 48.8% | $2.20 | 46702 | 77 | 22.2 | Other |
| 45 | GPT-5.6 Luna Medium | 47.7% | $0.39 | 7095 | 28 | 122.3 | Other |
| 46 | Sonnet 5 Low | 47.7% | $1.30 | 16269 | 33 | 36.7 | Other |
| 47 | Gemini 3.6 Flash Low | 47.4% | $1.13 | 20529 | 50 | 41.9 | Other |
| 48 | GPT-5.6 Terra Low | 46.9% | $0.53 | 5312 | 19 | 88.5 | Other |
| 49 | GPT-5.5 Low | 46.6% | $0.98 | 5168 | 20 | 47.6 | Other |
| 50 | GPT-5.6 Luna Low | 37.6% | $0.16 | 3209 | 17 | 235.0 | Other |

---

## 5. 决策树与总结

### 决策树

```
开始
 ├─ 日常小改 / 常规 Agent？     → Composer 2.5
 ├─ 多文件、要更聪明、还想省？ → Grok 4.5 High（关 Fast）
 ├─ 难关 / 架构 / 反复失败？   → Opus 5 High 或 Sol Max
 ├─ 只要极便宜、接受更弱？     → Luna / Terra Medium
 └─ 只要快、不差钱？           → 开 Fast（注意账单）
```

### 核心结论

1. **日常默认：Composer 2.5** — $0.44/任务，大额度池，性价比甜点。  
2. **升级首选：Grok 4.5 High（关 Fast）** — 分数近 Opus 5 High，成本约其四成；榜分有 caveat，能力仍值得用。  
3. **攻坚：Opus 5 / Sol Max** — 最强档，贵，适合关键时刻。  
4. **不当默认：Gemini 3.6、Kimi、Fable/Sonnet Max、各类 Fast。**

### 一句话

> **Composer 干活，Grok 升级，Opus/Sol 攻坚；Fast 慎开，Gemini/Kimi 不当默认。**

### 工作流

```
Composer 2.5
  → 不够 → Grok 4.5 High（关 Fast）
    → 仍不够 → Opus 5 High / Sol Max
```

### 读数提示

- Score = Agent 任务成功率，不是纯 chat 智商  
- Cost/task 比单价更能反映真实花费  
- Steps 高 = 更易空转烧额度  
- 小分差可能无意义，看档位差（如 56% vs 66%）

---

## 6. 附录：单价与 Fast

### 6.1 Fast / 特殊变体

| 模型 | Input $/M | Output $/M | 备注 |
|------|----------:|-----------:|------|
| Grok 4.5 普通 | 2 | 6 | 推荐 |
| Grok 4.5 Fast | 4 | 18 | 输出约 3× 贵 |
| Composer 2.5 普通 | 0.5 | 2.5 | 日常默认 |
| Composer 2.5 Fast | 3 | 15 | 明显更贵 |
| Opus 4.7 Fast | 30 | 150 | 极贵 |
| GPT-5.6 系 Fast | ≈2× | ≈2× | Sol/Terra/Luna |

### 6.2 完整单价（官方定价页）

| 模型 | Provider | Input | Cache Write | Cache Read | Output | 池 |
|------|----------|------:|------------:|-----------:|-------:|----|
| Composer 2.5 | Cursor | 0.5 | — | 0.2 | 2.5 | Cursor |
| Grok 4.5 | Cursor | 2 | — | 0.5 | 6 | Cursor |
| Auto Cost | Cursor | 1.25 | 1.25 | 0.25 | 6 | — |
| Composer 1 | Cursor | 1.25 | — | 0.125 | 10 | — |
| Claude Opus 5 | Anthropic | 5 | 6.25 | 0.5 | 25 | Other |
| Claude Fable 5 | Anthropic | 10 | 12.5 | 1 | 50 | Other |
| Claude Opus 4.8 | Anthropic | 5 | 6.25 | 0.5 | 25 | Other |
| Claude 4.7 Opus | Anthropic | 5 | 6.25 | 0.5 | 25 | Other |
| Claude Opus 4.7 Fast | Anthropic | 30 | 37.5 | 3 | 150 | Other |
| Claude Sonnet 5 | Anthropic | 3† | 3.75 | 0.3 | 15† | Other |
| Claude 4.6/4.5 Sonnet | Anthropic | 3 | 3.75 | 0.3 | 15 | Other |
| Claude 4.6/4.5 Opus | Anthropic | 5 | 6.25 | 0.5 | 25 | Other |
| Claude 4.5 Haiku | Anthropic | 1 | 1.25 | 0.1 | 5 | Other |
| GPT-5.6 Sol | OpenAI | 5 | 6.25 | 0.5 | 30 | Other |
| GPT-5.6 Terra | OpenAI | 2.5 | 3.125 | 0.25 | 15 | Other |
| GPT-5.6 Luna | OpenAI | 1 | 1.25 | 0.1 | 6 | Other |
| GPT-5.5 | OpenAI | 5 | — | 0.5 | 30 | Other |
| GPT-5.4 | OpenAI | 2.5 | — | 0.25 | 15 | Other |
| GPT-5.4 Mini | OpenAI | 0.75 | — | 0.075 | 4.5 | Other |
| GPT-5.4 Nano | OpenAI | 0.2 | — | 0.02 | 1.25 | Other |
| GPT-5.3/5.2 Codex 等 | OpenAI | 1.75 | — | 0.175 | 14 | Other |
| GPT-5 / Codex 系 | OpenAI | 1.25 | — | 0.125 | 10 | Other |
| GPT-5 Mini / Codex Mini | OpenAI | 0.25 | — | 0.025 | 2 | Other |
| GPT-5 Fast | OpenAI | 2.5 | — | 0.25 | 20 | Other |
| Gemini 3.6 Flash | Google | 1.5 | — | 0.15 | 7.5 | Other |
| Gemini 3.5 Flash | Google | 1.5 | — | 0.15 | 9 | Other |
| Gemini 3.1 Pro | Google | 2 | — | 0.2 | 12 | Other |
| Gemini 3 Pro / Flash 等 | Google | 见官网 | — | — | — | Other |
| GLM 5.2 | Z.ai | 1.4 | — | 0.26 | 4.4 | Other |
| Kimi K2.7 Code | Moonshot | 0.95 | — | 0.19 | 4 | Other |

† Sonnet 5 促销至 2026-08-31：Input $2 / Output $10。  
Teams/Enterprise 第三方另加 $0.25/M Cursor Token Rate（Composer/Grok 免收）。

### 6.3 来源

- https://cursor.com/docs/models-and-pricing.md  
- https://cursor.com/cursorbench  
- https://cursor.com/docs/models/grok-4-5  
- https://cursor.com/blog/composer-2-5  
