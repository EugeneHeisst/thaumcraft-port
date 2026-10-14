# Charging wand tooltip regression

2026-10-12: user reports unreadable jumping vis numbers while charging a wand in the arcane workbench.

Native ChargerAudit generates the real wand tooltip and measures its numeric line using Minecraft font for 10.00..10.95 vis at the original five-centivis charging interval, all six primals. Before fix width varies 122..206 pixels; the assertion fails. After fixed two-decimal amount formatting width is 206..206 and passes. Recipe-cost formatting is unchanged. Width can still grow normally when the integer part gains a digit.

Hidden silent `runChargerAudit` also passes actual network connection, five-centivis increments, capacity clamp, full-wand no-drain, workbench synchronization, attunement persistence, disconnected-source refusal and wand removal. This proves the formatting regression, not all possible causes of displayed value rollback.

Evidence: `/tmp/tc4-charger-tooltip-before.log`, `/tmp/tc4-charger-tooltip-after.log`, `/tmp/tc4-charger-tooltip-build.log`. Full build passes 21 tasks. Maven JEI API download timed out; used existing `-PjeiLocalJar` support with matching installed 19.57.0.451 JAR. No production/client installation or restart.
