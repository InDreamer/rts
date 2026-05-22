# Ask User Questions Contract

`ask-user-questions.json` is the portable handoff format before invoking askUserQuestionTool.

Ask the user only when:

- source evidence conflicts and cannot be resolved by further inspection
- business priority or source authority needs human decision
- KB truth or MVP completion would otherwise choose between plausible interpretations
- missing source access blocks truth and the user can provide direction

Do not ask when:

- the answer can be found by reading existing source
- the question is only about whether the agent should continue
- the question is implementation convenience rather than truth authority

Question severity:

```text
blocking
important
clarifying
```

Each question must have:

- `question_id`
- `severity`
- `object_refs`
- `claim_refs`
- `question`
- `why_needed`
- `options`
- `freeform_allowed`
- `blocks`

If invoking askUserQuestionTool, ask at most 1 to 3 blocking questions per round.
