# Local Pipeline Ops Reference

These ops are lightweight semantic labels used inside this draft pack's `logic.pipeline` sections.

- `exists`: returns true when the referenced source node is present.
- `read_xpath`: reads a source value from a named FpML XPath.
- `concat`: concatenates string inputs with no separator unless a template says otherwise.
- `equals`: compares two string values for exact equality.
- `branch`: executes the `then` branch when the condition is true; otherwise executes the `else` branch.
- `assign`: copies a value from a previously read or computed field.
- `constant`: emits or stores a literal value.
- `compose_key`: builds a lookup key from named values using a string template.
- `lookup_value`: reads a named field from a lookup definition.
- `invoke_helper`: calls a named helper definition and exposes named outputs for later pipeline steps and target binding.
- `coalesce`: returns the first non-empty value from the supplied arguments.
- `emit_block`: declares that a target block is rendered when its condition is satisfied.
- `emit_value`: declares the final target value written to a target XPath.

## Target Binding Conventions

- `using_inputs`: makes rule-to-lookup/helper input binding explicit without repeating lookup key construction or helper internal logic.
- `from_lookup`: indicates that a target emit binds to a named field returned by a preceding `lookup_value` step.
- `from_helper`: indicates that a target emit binds to a named field exposed by a preceding `invoke_helper` step.

These op names and binding conventions are descriptive only. They are not executable runtime instructions.
