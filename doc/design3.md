# Design 3: probabilistic project estimation, built in steps

This document is the theory behind a project estimation app. It has
three parts:

1. **State of the art.** What is known about planning and estimating
   project time: the mathematics, the psychology of the people giving
   the estimates, and the social setting in which estimates are used.
2. **Gaps.** Places where current practice gives wrong or misleading
   answers. Each gap comes with a proof or a worked demonstration, and
   with what a probabilistic approach does about it.
3. **Development path.** A sequence of versions. The first is a plain
   network scheduler, close to what existing apps do. Each later
   version adds one idea, and is adopted only if data from earlier
   versions shows that it forecasts better.

Every version must be usable by itself, and every version records the
data that the next one needs. No code is specified. Each step gives
its mathematical model and its grounds in logic, probability,
psychology and sociology.

Sources are cited by author and year, and every source is listed at
the end of this document.

## Notation

- `V`: the set of tasks in a project.
- `E`: the set of dependencies. `(u, v) ∈ E` means v cannot start until u finishes.
- `e_v`: the estimate a person gives for task v, in working days.
- `D_v`: the actual duration of task v. Before the task is done, it is a random variable.
- `R_v = D_v / e_v`: the ratio of actual to estimate. `R_v = 1.5` means 50% over.
- `S_v`, `F_v`: the start and finish times of v.
- `ln`: the natural logarithm.
- `N(μ, σ²)`: the normal distribution with mean μ and variance σ².
- `P(·)`, `E[·]`: probability and expected value.
- `Q_p`: the p-quantile of a forecast, also written P50, P80 and so on.
  `Q_0.8` is the time the project finishes by with probability 0.8.
- `Φ`: the cumulative distribution function of `N(0, 1)`.

A random variable is lognormal when its logarithm is normal.
If `ln R ~ N(μ, σ²)`, then the median of R is `e^μ` and its mean is `e^(μ + σ²/2)`.
The mean is always larger than the median.

---

# Part 1. State of the art

## 1.1 Networks and dates

A project is drawn as a directed acyclic graph `G = (V, E)`.
Each task has one duration `d_v`.
The critical path method (Kelley 1959) computes the earliest dates with one pass in topological order:

    S_v = max { F_u : (u, v) ∈ E }      (S_v = 0 if v has no predecessors)
    F_v = S_v + d_v
    project finish = max_v F_v

The longest path through the graph is the critical path.
Its length is the project duration.
Slack, or float, is how far a task can move without moving the finish.

The precedence diagramming method adds start-to-start, finish-to-finish and start-to-finish links, each with a lag.
All of these are bounds on the difference between two time points, and they can be solved as a simple temporal problem (Dechter 1991).

Almost every commercial planning tool computes dates this way.
The durations are single numbers, and the output is a single date.

## 1.2 PERT

PERT (Malcolm 1959) asks for three numbers per task: optimistic `a`, most likely `m` and pessimistic `b`.
It treats the duration as a beta distribution and approximates its moments:

    mean      = (a + 4m + b) / 6
    std. dev. = (b − a) / 6

It finds the critical path using the means.
It then adds the means and variances along that one path, and uses the central limit theorem to treat the total as normal.
Its output is a probability of finishing by a given date.

## 1.3 Why PERT is optimistic, and simulation

PERT looks at one path only.
Where several paths join, the finish time is the maximum of several random variables,
and the expected maximum is larger than the largest expected value.
MacCrimmon 1964 analysed the errors in the PERT assumptions.
Clark 1961 gave formulas for the moments of the maximum of normal variables.
Van Slyke 1963 replaced the analysis with Monte Carlo simulation:

1. draw a duration for every task
2. run the forward pass
3. repeat many times, and read probabilities off the results

Simulation also gives the criticality index, which is the probability that a task lies on the critical path.

## 1.4 Simulation in practice

Monte Carlo schedule risk analysis is now a standard technique for large projects (Herroelen 2005).
Experts give a distribution for each task, usually triangular or beta.
Correlations between tasks may be stated, but they usually come from judgement and are often left out.
The distributions are rarely checked against how earlier projects actually turned out.

## 1.5 Buffers

Critical chain (Goldratt 1997) takes the safety out of each task estimate and pools it into buffers at the end of a chain.
The argument is behavioural. Safety inside a task is used up: work expands to fill the time available (Parkinson 1955),
and people start late when they know there is slack. Herroelen 2001 reviews the merits and the weak points,
including how buffer sizes are set by rules of thumb rather than by data.

## 1.6 Estimating software work

- **Parametric models** predict effort from size measures, for example COCOMO (Boehm 1981).
- **Expert judgement** dominates in practice.
  A review of surveys found that 60–80% of software projects overrun effort or schedule, that average overruns are 30–40%,
  and that there is no evidence that formal models give more accurate estimates (Moløkken 2003).
- **Relative estimation.** Agile teams estimate in story points and forecast from velocity,
  the points completed per iteration (Cohn 2005).
  Planning poker has a team reveal their estimates at the same moment,
  so that no one anchors on another's number (Grenning 2002).
- **The cone of uncertainty** is the idea that estimates narrow as a project proceeds.
  It was drawn from judgement in Boehm 1981. Little 2006 studied 106 commercial projects at one company
  and found that actual schedules ran about twice the initial estimate on average,
  and that the ratio of actual to estimate fitted a lognormal distribution better than a normal one.
  These figures come from secondary accounts of the paper.
- **Evidence-based scheduling** (Spolsky 2007) records, for each developer, the ratio between estimated and actual time on past tasks.
  It forecasts a ship date by Monte Carlo, drawing those historical ratios at random.
  Of the methods surveyed here, it is the closest to learning from data.
- **Bayesian methods** in the research literature include forecasting project duration from progress with Bayesian updating (Kim 2009)
  and Bayesian networks for project risk (Fenton 2018).

## 1.7 Psychology of predicting time

**The planning fallacy.** People underestimate how long their own tasks will take,
even when they know that similar tasks ran late before (Kahneman 1979, Buehler 1994).

In Buehler 1994 study 1:
- 37 students predicted when they would finish their honours theses
- the average best guess was 33.9 days, and the average actual time was 55.5 days
- 29.7% finished by their best guess
- even the average "if everything goes as poorly as it possibly could" prediction, 48.6 days, was earlier than the actual time
- the predictions still correlated strongly with the actual times, r = 0.77

So the estimates were informative but biased.
This is the case in which statistical correction pays off most.

**Inside and outside views.** Predictions built from the plan of the task at hand (the inside view) are optimistic.
Predictions built from how similar past tasks turned out (the outside view) are more accurate (Kahneman 1993).
Roy 2005 adds that people also misremember how long past tasks took,so even an honest look back is biased
unless the past durations were recorded.

**Overconfident ranges.** When people give a range they are "almost sure" of,
the actual value falls outside it far more often than they claim:

- Students' 98% ranges for programming tasks contained the actual effort about 60% of the time.
  Training in setting the limits raised this to about 70% (Connolly 1997, as reported in Jørgensen 2004).
- In three industrial software projects, ranges meant to be "very unlikely" to miss
  contained the actual effort for 33%, 44% and 21% of tasks.
  Wider ranges were not more likely to contain the actual value,
  so the width people chose said little about their real uncertainty (Jørgensen 2004).
- People asked for 90% intervals produce intervals that they themselves judge to be about 50% certain (Teigen 2005).

**Anchoring.** A number seen before estimating pulls the estimate toward it,
even when the number comes from someone who clearly knows nothing (Tversky 1974, Aranda 2005).

**Unpacking.** Listing the parts of a task before estimating it makes the estimate longer, and it reduces the planning fallacy (Kruger 2004).
Decomposition alone did not make ranges less overconfident in Connolly 1997.

Halkjelsvik 2012 is a broad review of all of this.

## 1.8 The social setting

- **Strategic misrepresentation.** In 258 transport infrastructure projects, costs were underestimated in about nine of ten.
  The authors argue that the pattern is better explained by deliberate optimism to win approval than by error (Flyvbjerg 2002).
  Love and Ahiaga-Dagbui dispute this reading.
- **Reference class forecasting** answers both honest optimism and misrepresentation the same way:
  forecast from the outcomes of a class of comparable past projects (Flyvbjerg 2006).
- **Estimate, target and commitment** are three different things:
  what we think will happen, what we would like to happen, and what we promise.
  In practice one number is often made to serve as all three (McConnell 2006).
- **Goodhart's law.** A measure that becomes a target stops measuring well (Goodhart 1975).
  An estimate that is used to judge people will be padded or trimmed to suit.
- **The MUM effect.** People are reluctant to pass on bad news (Rosen 1970), so slips are reported late.
- **Combining opinions.** The average of several independent estimates is usually better than a typical single one,
  but people undervalue averaging (Larrick 2006).

## 1.9 Judging a forecast

A forecast that is a probability distribution can be scored once the outcome is known.

- **Calibration.** Of all the 80% intervals issued, about 80% should contain the outcome.
- **Proper scoring rules** reward a forecaster most for reporting what they actually believe (Brier 1950, Gneiting 2007).
  The log score of a forecast with density `f` is `ln f(actual)`.
  Higher is better, and the average over many forecasts compares two methods.
- **The probability integral transform.** For each outcome, record `u = F(actual)`, where `F` is the forecast's cumulative distribution.
  If the forecasts are calibrated, the values of u are uniform on [0, 1].
  If too many land near 0 or 1, the forecasts are too narrow.

## 1.10 Summary

- **Critical path.**Output: one date. Uncertainty: none. Learns from outcomes: no.
- **PERT.** Output: a probability, on one path. Uncertainty: from three-point estimates. Learns from outcomes: no.
- **Monte Carlo risk analysis.** Output: a distribution. Uncertainty: from expert distributions. Learns from outcomes: rarely.
- **Critical chain.** Output: a date plus a buffer. Uncertainty: by rule of thumb. Learns from outcomes: no.
- **Velocity or story points.** Output: a date range. Uncertainty: from the spread of velocity. Learns from outcomes: yes, at team level.
- **Evidence-based scheduling.** Output: a distribution. Uncertainty: from each person's history. Learns from outcomes: yes.
- **Reference class forecasting.** Output: a distribution, or an uplift. Uncertainty: from a class of past projects. Learns from outcomes: yes, across projects.

---

# Part 2. Gaps

Each gap is stated, then proved or demonstrated, and then followed by what a probabilistic model does about it.
The numbers were computed by simulation with 1–2 million runs.

## Gap 1. A single-number schedule is optimistic at every join

**Claim.** If a task waits for several uncertain predecessors,
the expected start of that task is later than the date the critical path method computes from expected durations.

**Proof.** Let X and Y be the finish times of two predecessors.
For every outcome, `max(X, Y) ≥ X` and `max(X, Y) ≥ Y`.
Taking expected values gives `E[max(X, Y)] ≥ max(E[X], E[Y])`.
The difference is `E[max(X, Y)] − E[X] = E[(Y − X)⁺]`.
This is strictly positive whenever `P(Y > X) > 0`,
that is, whenever the predecessors could finish in either order.

**Demonstration.** Each parallel task takes `N(10, 3²)` days.
The critical path method says the join happens at day 10.

| Parallel tasks | Expected join | P(join ≤ 10) |
|----------------|---------------|--------------|
| 2              | 11.7          | 0.25         |
| 10             | 14.6          | 0.001        |

**What a probabilistic model does.** It simulates the joins instead of adding means.
This needs no new input, only a different calculation.

## Gap 2. Adding typical durations gives an untypical total

**Claim.** If each estimate is the median of a right-skewed duration, the sum of the estimates is below the median of the total.
The probability of meeting the summed plan falls toward 0 as tasks are added.

**Proof sketch.** For a lognormal task, the median is `m` and the mean is `m e^(σ²/2) > m`.
For n independent tasks, the mean total is `n m e^(σ²/2)`.
The standard deviation of the total grows only like √n, while the gap between the mean total and `n m` grows like n.
So by Chebyshev's inequality, `P(total ≤ n m) → 0`.

**Demonstration.** Ten tasks in sequence. Each has median 5 days with `ln R ~ N(0, 0.5²)`, so every estimate is honest.

|                       | Days |
|-----------------------|------|
| Plan (sum of medians) | 50   |
| Median of the total   | 55.8 |
| P80 of the total      | 64.2 |
| P(total ≤ 50)         | 0.25 |

So a plan made from honest "50/50" estimates has a 25% chance of being met.

**What a probabilistic model does.** It treats durations as skewed distributions and reports quantiles of the total.

## Gap 3. Assuming independence makes large plans look precise

**Claim.** If task durations share a cause, such as a team that
underestimates everything or an unfamiliar technology, then the
spread of the total does not shrink with more tasks the way an
independent model predicts.

**Proof.** For n tasks with variance σ² each and pairwise correlation
ρ:

    Var(ΣD) = n σ² (1 + (n − 1) ρ)

so the relative spread `sd / mean` tends to `(σ / μ) √ρ` as n grows,
not to 0. A model with ρ = 0 promises precision that the project does
not have.

**Demonstration.** 20 tasks, each with median 5, and the same total
log variance in both cases (0.5²). In the shared case, a project-wide
factor carries 0.3² of that variance.

| Model | P10 | P50 | P90 | P90 / P50 |
|---|---|---|---|---|
| independent | 97 | 112 | 131 | 1.17 |
| shared cause | 72 | 108 | 161 | 1.50 |

**What a probabilistic model does.** It includes a shared
project-level factor, and estimates how large that factor is from
past projects (step 3).

## Gap 4. Estimates are biased and too sure, and tools do not learn

**Evidence.**

- Overruns are the rule: 60–80% of projects overrun, by 30–40% on
  average (Moløkken 2003).
- Students' actual times were 64% longer than their best guesses, and
  only 30% of the guesses were met (Buehler 1994).
- Ranges meant to be near certain held the actual value for 21–44% of
  tasks (Jørgensen 2004).

**The missing feedback.** Jørgensen 2004 looked at more than 100
projects in 6 organisations. About 30% gave ranges for their tasks,
but only 3 recorded actual effort in a form that could be compared
with the estimates. Without that record no tool can learn, and people
fall back on biased memory (Roy 2005).

**Demonstration that correction pays.** Buehler 1994 found r = 0.77
between prediction and outcome. When estimates are strongly
correlated with outcomes but biased, a correction learned from
history keeps the information and removes the bias. Step 1 makes
this precise.

**What a probabilistic model does.** It records each estimate and
each outcome, learns the distribution of `R = actual / estimate`, and
applies it to new estimates. This is the outside view done by
machine.

## Gap 5. Progress does not teach the plan

**Claim.** When early tasks overrun, critical path tools move the
later tasks by the amount of the slip and nothing more. If the
overrun is partly a shared cause, the remaining estimates should also
grow.

**Demonstration, by Bayes' rule with a normal prior and normal
noise.** Suppose the project factor is `c ~ N(0, 0.3²)` and each task
has `ln R_i = c + ε_i`, with `ε_i ~ N(0, 0.4²)`. Three tasks finish,
each with `ln R = 0.5`, that is, 65% over. The posterior is:

    precision = 1/0.3² + 3/0.4² = 29.9
    mean of c = (3 × 0.5 / 0.4²) / 29.9 = 0.31
    sd of c   = 1 / √29.9 = 0.18

The remaining tasks are now expected to run `e^0.31 ≈ 1.37` times
their estimates. A deterministic tool expects 1.0.

**What a probabilistic model does.** It updates shared factors from
every task that finishes, mechanically, without waiting for anyone to
report the slip (step 3).

## Gap 6. Rare events vanish into averages

**Claim.** A risk that probably will not happen is left out of a
single-number plan. Several such risks together probably will
happen.

**Demonstration.** Five tasks each have a 10% chance of needing
rework. The chance that at least one needs rework is
`1 − 0.9⁵ = 0.41`. The distribution of the outcome has two humps,
and its mean describes neither of them.

**What a probabilistic model does.** It models a risk as an event
with a probability and an impact. The probability is learned from how
often such events occurred before (step 5).

## Gap 7. One number is asked to be estimate, target and commitment

**Claim.** If the cost of being late differs from the cost of being
early, the best date to promise is a quantile of the forecast, not
its centre.

**Derivation.** Say each day of promised time beyond the actual
finish costs `c_o`, and each day the actual finish runs past the
promise costs `c_u`. The expected cost of promising date t is
minimised where `P(T ≤ t) = c_u / (c_u + c_o)`. This is the
newsvendor result (Arrow 1951). If lateness costs four times as much
as idle slack, promise the P80.

**Sociology.** Once the estimate is a distribution and the promise is
a stated quantile, the negotiation changes. It is no longer about the
number ("can you do it in 10 days?") but about the level of
confidence ("how sure do we need to be?"). This separates honest
forecasting from the pressure that drives misrepresentation
(Flyvbjerg 2002, McConnell 2006).

## Which methods address which gaps

| Gap                  | CP   | PERT | MC   | EBS  | RCF  |
|----------------------|------|------|------|------|------|
| 1 joins              | no   | no   | yes  | yes  | no   |
| 2 skew               | no   | part | yes  | yes  | yes  |
| 3 shared causes      | no   | no   | (a)  | no   | part |
| 4 learning bias      | no   | no   | no   | (b)  | (c)  |
| 5 progress updates   | (d)  | no   | no   | part | no   |
| 6 rare events        | no   | no   | (e)  | no   | part |
| 7 commitment level   | no   | part | yes  | yes  | yes  |

Columns:

- CP: critical path
- PERT: PERT
- MC: Monte Carlo risk analysis
- EBS: evidence-based scheduling
- RCF: reference class forecasting

Notes:

- (a) only if the shared causes are stated
- (b) yes, per person
- (c) yes, per reference class
- (d) it only shifts later tasks by the slip
- (e) only if the events are modelled

No method surveyed covers all seven gaps. The ones that learn from
data either ignore network structure or ignore shared causes. The app
proposed here closes the gaps one at a time, in an order set by what
the data shows.

---

# Part 3. Development path

## 3.0 Rules for the path

1. **Each version is usable alone.** A team should want to use it even if no later version is ever built.
2. **Each version records what the next one needs.** The main product of the early versions is a clean record of estimates and outcomes.
3. **Facts are recorded, never overwritten.** Every estimate, start, finish and change is stored with its time and author,
   and nothing is edited in place, as in [CONTRIBUTING.md](../CONTRIBUTING.md).
   Any past forecast can then be recomputed exactly and scored honestly, with no hindsight.
4. **Forecasts are recorded too.** Every forecast the app shows is stored, so that it can be scored against the outcome.
5. **A step is adopted on evidence.** A new model replaces the old one only if it scores better on tasks it was not fitted to (3.1).
   Being more general is not a reason to adopt it.

## 3.1 How each step is tested

**Out-of-time testing.** Choose a cut date. Fit the model on tasks that finished before the cut,
and forecast tasks that started after it. Repeat for several cut dates. This mirrors real use, in which the future is never seen.

**Scores.**

- mean log score, `(1/n) Σ ln f_i(actual_i)`. This is the main criterion, and higher is better
- coverage of the 50% and 80% intervals, which should be near 50% and 80%
- a histogram of the PIT values, which should be flat

**How many tasks are needed.** If task log-ratios vary with standard deviation σ ≈ 0.5,
the mean log-ratio is known to within ±0.1 (about ±10% on durations) at 95% confidence once

    n ≥ (1.96 × 0.5 / 0.1)² ≈ 96 completed tasks

The relative standard error of the estimated σ is about `1 / √(2(n − 1))`, which is 10% at n = 50.
These are the first milestones for data collection.

**What cannot be tested this way.** People change how they estimate once they see the app's corrections.
This reactivity shows up as drift over time in the ratio R. It is tracked in every step, and it is handled in step 2.

## The running example

Five tasks:

| Task | Meaning         | Estimate (days) | Waits for |
|------|-----------------|-----------------|-----------|
| A    | design          | 3               | —         |
| B    | build back end  | 5               | A         |
| C    | build front end | 4               | A         |
| D    | integrate       | 2               | B, C      |
| E    | test            | 3               | D         |

---

## Step 0. A plain network scheduler that records everything

**Adds.** Tasks, single-number estimates, finish-to-start dependencies, the critical path, and a record of actual starts and finishes.
This is a deliberately plain version of existing tools.

**Model.** The critical path method of section 1.1, with `d_v = e_v`.
In the example: A 0–3, B 3–8, C 3–7, D 8–10, E 10–13.
The project finishes on day 13, and the critical path is A, B, D, E.

Once a task has an actual start or finish, the actual value replaces the computed one, and the forward pass runs again from there.

**Inputs and outputs.**

- In: tasks, estimates, dependencies, and the actual start and finish of each task.
- Out: dates and the critical path, the same as existing tools.

**Logic and probability.**

- The graph must be acyclic. Topological sorting (Kahn 1962) both orders the forward pass and detects cycles.
- The estimate is given a defined meaning: *the number of working days you think is equally likely to be too high or too low*.
  That makes it a median. The app does not use this meaning yet, but the question asked must be the same in every version,
  or the data cannot be compared. Jørgensen 2004 notes that unclear wording makes estimates impossible to evaluate.
- The actual duration is defined as working days from the recorded start to the recorded finish. The unit is stored with every number.

**Psychology.**

- The person who will do the task gives the estimate, and the app records who it was.
  People are more optimistic about their own tasks than observers are (Buehler 1994).
  The app does not try to correct this here. It records it, so that step 2 can measure it.
- No other estimate is shown before a person enters theirs, to avoid anchoring (Tversky 1974, Aranda 2005).
  This is the same idea as the simultaneous reveal in planning poker.
- An estimate counts only if it was recorded before the task started. Later revisions are kept, but they are marked as revisions.
  Estimates made after the fact are contaminated by hindsight.

**Sociology.**

- The tool must cost less effort than it saves, or the outcome records will be missing.
  That is the failure Jørgensen 2004 found in 97 of 100 projects. Recording a start or a finish must be a single act.
- Estimates are not used to evaluate people. If they are, Goodhart's law applies and the data is spoiled before step 1 can use it.

**Data recorded.** For every task: the estimate with its author and time, all revisions, the actual start and finish,
the dependencies, and an optional kind label (for example design, build or test).
When a task starts later than its predecessors allow, the reason is recorded as a short category, such as "waiting for a person",
"waiting for an outside party" or "not in the plan". Step 6 uses these reasons.

**Experiment for step 1.** Once about 50–100 tasks are complete, compute `r_i = ln(actual_i / estimate_i)`.

- Hypothesis H1: the values `r_i` are roughly normal, with a mean above 0 (systematic underestimation) and a standard deviation between 0.3 and 0.7.
- Check with a histogram, a normal quantile plot, and the mean and standard deviation.

If H1 holds, step 1 is justified. If the tail is much heavier than normal, note it for step 5.

**Limits.** Single dates are optimistic (gaps 1 and 2), and there is no learning (gap 4).

---

## Step 1. Calibrated forecasts from the team's history

**Adds.** Every estimate is turned into a distribution, using how the
team's past estimates turned out. Dates become probabilities. This is
reference class forecasting, done automatically and continuously.

**Model.** For a new task with estimate e:

    D = e × R,    ln R ~ N(μ, σ²)

μ and σ are estimated from the completed tasks in the history, where
`r_i = ln(a_i / e_i)`:

    μ̂ = (1/n) Σ r_i
    σ̂² = (1/(n−1)) Σ (r_i − μ̂)²

To allow for the uncertainty in μ̂ and σ̂, the forecast for a new
task uses the predictive distribution:

    ln R ~ μ̂ + σ̂ √(1 + 1/n) · t_(n−1)

where t is Student's t distribution. For n above about 30 this is
close to normal. It is the standard result for a normal model with an
uninformative prior (Gelman 2013).

**Before there is enough history**, μ and σ start from published
outside-view values, for example an overrun of about 30% (Moløkken
2003), so `μ ≈ ln 1.3 ≈ 0.26`, with `σ ≈ 0.5`. This prior is treated
as worth only a few tasks, so that local data takes over quickly.
With a normal prior on μ, the update is the formula from gap 5.

**Unfinished tasks.** A task that has been running for t days without
finishing tells us that `D > t`. Leaving such tasks out biases μ̂
low, because the slow tasks are the ones still open. Each one
therefore adds the term `ln P(ln R > ln(t/e)) = ln(1 − Φ((ln(t/e) −
μ)/σ))` to the log likelihood, and μ and σ are fitted by maximum
likelihood. This is the censoring used in survival analysis.

**Project forecast.** By Monte Carlo:

1. draw `R_v` for every unfinished task
2. set `D_v = e_v R_v`, and for a running task draw D_v conditional
   on `D_v > elapsed`
3. run the forward pass
4. repeat N times

Report quantiles of the finish, the probability of finishing by any
date, and each task's criticality index. With N = 10,000 runs, any
reported probability has a Monte Carlo standard error of at most
`√(0.25 / 10,000) = 0.005`.

**The example**, with a history that gives μ = 0.3 and σ = 0.5:

| Forecast | Finish |
|---|---|
| Step 0 | day 13 |
| Step 1: P50, P80, P95 | 20.3, 25.2, 31.3 |
| Step 1: P(finish ≤ 13) | 0.03 |
| If the estimates were unbiased (μ = 0): P50, P(≤ 13) | 15.1, 0.28 |

The last row shows gaps 1 and 2 on their own: even with honest
estimates, day 13 has a 28% chance.

**Commitments.** The app reports quantiles. Which quantile becomes the
promise is a decision for the people involved. Gap 7 gives the rule
when the costs of being late and being early can be stated.

**Logic and probability.**

- The model has two parameters, and both are estimated from data.
- Rescaling every estimate by the same learned factor removes the
  systematic bias, and it loses none of the information in the
  estimates, because the correlation between `ln e` and `ln a` is
  unchanged. That simple statistical rules match or beat holistic
  judgement is a classic finding (Dawes 1988, cited in Jørgensen
  2004).
- Durations are independent in this step. Gap 3 is still open.

**Psychology.**

- This is the outside view (Kahneman 1993). People keep estimating
  in their usual way, and the correction is applied afterwards. That
  works even though people neglect their own history (Buehler 1994)
  and misremember it (Roy 2005).
- The raw estimate and the calibrated forecast are both kept and both
  shown, so that a person's own number is never silently replaced.
- After seeing corrections, people may inflate their estimates. Since
  μ is relearned continuously, a consistent change in estimating
  habit is absorbed. Only inconsistency, an increase in σ, costs
  accuracy. Drift is watched (3.1).

**Sociology.**

- The correction is learned at team level, not per person. This
  avoids blame while trust in the tool is being built.
- The conversation moves from "how long" to "how sure" (gap 7).

**Data recorded.** Everything from step 0, plus every forecast issued
(its quantiles and the model version) so it can be scored.

**Experiment for step 2.**

- H2a: step 1's 80% intervals cover 75–85% of outcomes, and it beats
  step 0 on log score.
- H2b: people or kinds of task differ. Fit `r_i` by person and by
  kind, and compute the share of variance between groups (the
  intraclass correlation). If it is above about 0.05 and the groups
  have enough tasks, step 2 is justified.
- H2c: drift. Regress `r_i` on the date the task started. A clear
  slope means that people are reacting to the tool.

**Limits.** All tasks are treated alike. There are no shared causes
and no rare events.

---

## Step 2. Different people and different kinds of task

**Adds.** Each person and each kind of task gets its own adjustment,
and the adjustments are pooled sensibly toward the team.

**Model.** A hierarchical, or multilevel, model:

    r_i = μ + a_person(i) + b_kind(i) + ε_i
    a_p ~ N(0, τ_a²),   b_k ~ N(0, τ_b²),   ε_i ~ N(0, σ²)

The variances τ_a², τ_b² and σ² are estimated from the data (Gelman
2013). For a person with n_p completed tasks and mean residual ȳ_p,
the estimate of `a_p` is a weighted average of that person's own mean
and the team's, which is 0:

    â_p = w ȳ_p,    w = n_p / (n_p + σ²/τ_a²)

A newcomer has `w = 0` and gets the team value. As tasks are
completed, w moves toward 1. This is partial pooling. It avoids both
"everyone is the same" and "trust three data points".

**Drift.** If H2c showed drift, μ is allowed to change slowly. Older
tasks are given less weight, for example with half-life h, and h is
chosen by out-of-time score.

**Optionally, spread by kind.** Some kinds of task are more uncertain
than others, so `σ_kind` is allowed to vary, with pooling in the same
way.

**Logic and probability.** Partial pooling is what Bayes' rule gives
when the groups are believed to be similar but not identical. The
weight w is a posterior precision ratio, which is the formula from
gap 5 again.

**Psychology.**

- Personal feedback is how calibration is learned, and calibration
  training does improve it (Connolly 1997). But feedback that compares
  people can feel threatening and can invite gaming.
- Each person sees their own adjustment as information about their
  own estimates, not as a score.

**Sociology.**

- Personal adjustments are visible only to that person. The team
  forecast uses them without displaying them.
- This keeps Goodhart pressure off the data, which the whole design
  depends on.

**Data recorded.** Unchanged, apart from the kind label, which now
matters. Kinds come from a small fixed list, so that groups have
enough tasks.

**Experiment for step 3.** Fit a project-level effect on historical
data with at least about 10 completed projects, and estimate its
variance τ_c².

- H3: τ_c² > 0, and the correlation `ρ = τ_c² / (τ_c² + σ²)` is about
  0.1 or more.
- Test: forecast each project's remaining work from cut points taken
  mid-project, with and without the project effect, and compare log
  scores.

**Limits.** Tasks within a project are still independent, and
progress updates only the tasks that are done.

---

## Step 3. Shared causes, and forecasts that learn during the project

**Adds.** A factor shared by the tasks of one project. Every finished
or overrunning task updates the forecast for all the remaining tasks.
This closes gaps 3 and 5.

**Model.**

    r_i = μ + a_person(i) + b_kind(i) + c_project(i) + ε_i
    c_j ~ N(0, τ_c²)

Before project j starts, `c_j` is unknown and has its prior
`N(0, τ_c²)`. Each completed task in j gives a residual
`y_i = r_i − μ − a − b`. With m completed tasks, the posterior is:

    precision(c_j) = 1/τ_c² + m/σ²
    mean(c_j)      = (Σ y_i / σ²) / precision(c_j)

Running tasks add censored information (step 1). The Monte Carlo
forecast draws `c_j` from its posterior once per run, and uses that
value for all of the run's remaining tasks. That shared draw is what
makes the durations correlated.

**The example**, with τ_c = 0.3 and σ = 0.4, so the total log
standard deviation is still 0.5. Task A finishes after 7 days instead
of 3:

| Forecast after A = 7 | Finish |
|---|---|
| Step 0: shift by the slip | day 17 |
| Step 1: independent | P50 22.7, P80 27.1 |
| Step 3: shared factor | P50 25.3, P80 31.2 |

The posterior mean of `c` is 0.20, so the remaining tasks are expected
to run about 22% longer than step 1 would say.

**Logic and probability.** This is the same normal update as gap 5,
applied inside the Monte Carlo. Exchangeability of tasks within a
project is the assumption that justifies one shared factor (Gelman
2013).

**Psychology.** People explain away early slips as one-off causes,
which keeps their optimism intact (Buehler 1994). The model does not
explain anything away. It weighs each slip by how much shared-cause
variance history has shown. The weight is a number estimated from
data, not a judgement.

**Sociology.** The forecast moves as soon as facts are recorded. No
one has to announce bad news, which reduces the cost of the MUM
effect (Rosen 1970). Because the movement follows a published rule,
it is harder to read as anyone's fault.

**Data recorded.** Unchanged. The value of the earlier recording
discipline shows here: the shared factor can only be estimated
because earlier projects were recorded completely.

**Experiment for step 4.** In a trial on a subset of tasks, ask for a
range as well as the estimate. Ask for the low and high values that
"would surprise you" before asking for the estimate, or after it,
alternately, to measure order effects. Compute the stated log-width
`w_i = ln(high_i / low_i)`.

- H4: tasks with wider stated ranges have larger `|r_i − predicted|`.
  The correlation between w_i and the absolute residual is positive,
  and adding w_i to the model improves the log score.
- Jørgensen 2004 found no such relation in industry. So this
  hypothesis may fail, and if it does, step 4 is skipped.

**Limits.** Every task has the same spread apart from its kind.
Discrete risks and alternatives are still missing.

---

## Step 4. People's own uncertainty, if it carries information

**Adds.** Each person's stated range adjusts the spread of that
task's forecast, but only to the extent that history shows stated
ranges predict actual error.

**Model.**

    ε_i ~ N(0, σ_i²),   ln σ_i = ln σ + β (w_i − w̄)

β is estimated from data. If β = 0, stated ranges are ignored, as the
evidence of Jørgensen 2004 suggests may happen. If β > 0, wider
stated ranges mean wider forecasts. The stated range is never taken
at face value, because a "90%" range is about a "50%" range in the
estimator's own judgement (Teigen 2005).

**An alternative way of asking**, tested here: rather than asking for
an interval at a fixed confidence, ask "what is the chance this is
done within X days?" for an X the app chooses. This gives a
probability that can be scored with the Brier score (Brier 1950). It
fits the finding that people judge a given interval better than they
produce one for a given confidence (Teigen 2005).

**Psychology.** Asking for a range costs effort and attention. It is
kept only if it pays off in accuracy. Asking for generous limits
reduced overconfidence in Connolly 1997, which is worth testing as
wording.

**Sociology.** A wide range can sound like a lack of competence. Since
stated ranges are calibrated rather than used directly, people can
state honest uncertainty without it turning into a wide date.

**Data recorded.** Ranges or probability judgements, together with
the wording and order used.

**Experiment for step 5.** Look at the tail of the residuals from
steps 3 and 4.

- H5: the largest overruns are more frequent than the lognormal model
  predicts. Check the proportion of residuals beyond 2σ̂ against the
  normal value of 4.6%.
- H5b: the large overruns are associated with recorded reasons, such
  as "not in the plan", rework or an outside party.

If both hold, step 5 is justified.

**Limits.** The spread is continuous. Large discrete events are still
smeared into the tail.

---

## Step 5. Risks and alternatives

**Adds.** Named events that may or may not happen, each with a
probability and an impact. Also points where the work may go one of
several ways.

**Model.** For risk k attached to task v:

    I_k ~ Bernoulli(p_k)               (the event happens or not)
    X_k = impact, lognormal like any duration
    D_v = e_v R_v + Σ_k I_k X_k

An alternative is a choice among paths with probabilities `q_1 … q_m`.
In each Monte Carlo run, one path is drawn and only its tasks are
scheduled.

**Where the probabilities come from.** For a kind of risk that
occurred s times in n past opportunities, the rule of succession
gives:

    p = (s + 1) / (n + 2)

This is the posterior mean with a uniform prior (Jaynes 2003). It is
never exactly 0 or 1, and it moves toward the observed frequency as
data grows. Before there is any history, a person's stated
probability is used as a prior worth a small number of observations.

**Logic and probability.** The forecast becomes a mixture
distribution, which may have several humps (gap 6). Risk probabilities
are scored with the Brier score once each risk is known to have
happened or not.

**Psychology.** Listing what could go wrong makes estimates longer
and more realistic, through unpacking (Kruger 2004). Listing risks is
also where availability works against accuracy: recent and vivid
risks get listed, and dull frequent ones are forgotten. A list of
risk kinds from the team's own history counters this.

**Sociology.** Naming a risk can sound like predicting failure, or
like an excuse in advance. Recording risks as events with learned
frequencies, rather than as warnings from a person, makes them part
of normal planning.

**Data recorded.** Risks named, whether each occurred, and its actual
impact. Alternatives, and which one was taken.

**Experiment for step 6.** From the reasons recorded since step 0,
measure how often tasks waited for something that was not in the
network: a person busy elsewhere, an outside party, or work that
overlapped rather than followed.

- H6: a material share of delay, say more than 10% of the total slip,
  comes from such unmodelled waits.

**Limits.** The structure is still finish-to-start, with no shared
people.

---

## Step 6. Richer structure, when the data asks for it

**Adds,** one at a time and each with its own test as in 3.1, the
following layers:

1. start-to-start and finish-to-finish links with lags, if H6 shows
   overlapping work
2. deadlines and maximum gaps, reported as probabilities of being met
   rather than enforced
3. calendars, if working time and elapsed time diverge in the data
4. shared people and machines: a task cannot start until its person
   is free. In simulation, a priority rule settles who goes first, and
   the rule is recorded as a decision
5. a general model of the project: questions whose answers
   change which work exists, and answers decided by timing. This comes
   last, and only if steps 0–5 leave a measurable share of outcomes
   unexplained by structure

**Logic and probability.** Each layer changes only the forward pass
inside the Monte Carlo. Steps 1–5 are untouched: the distributions of
R, the pooling, the shared factor and the risks. That separation is
what makes it possible to add one layer at a time and test each one.

**Psychology and sociology.** Who gets a shared person first is a
political question as well as a technical one. The app records the
rule used and who chose it, and shows what the rule costs in forecast
terms. It does not decide.

---

## Summary of the path

- **Step 0.** Model: critical path on the estimates.
  New parameters: none. Needs: nothing.
  Adopted: always, because it creates the data.
- **Step 1.** Model: `D = e R`, with R lognormal.
  New parameters: μ, σ. Needs: about 50–100 finished tasks.
  Adopted when: it beats step 0 on log score, with calibrated
  intervals.
- **Step 2.** Model: adjustments by person and by kind, pooled.
  New parameters: τ_a, τ_b, drift. Needs: groups with enough tasks.
  Adopted when: the share of variance between groups is about 0.05 or
  more, and it beats step 1.
- **Step 3.** Model: a shared project factor, with live updates.
  New parameters: τ_c. Needs: about 10 or more finished projects.
  Adopted when: ρ is about 0.1 or more, and it beats step 2
  mid-project.
- **Step 4.** Model: stated ranges set the spread.
  New parameters: β. Needs: a range trial.
  Adopted when: β > 0, and it beats step 3.
- **Step 5.** Model: risk events and alternatives.
  New parameters: p_k and impacts. Needs: risk records.
  Adopted when: the heavy tail is tied to events, and it beats step 4.
- **Step 6.** Model: richer structure, up to a general model.
  New parameters: layer by layer. Needs: the recorded wait reasons.
  Adopted when: unmodelled waits are material.

Each step is one idea that a computer science undergraduate can state
in a line of mathematics: a graph pass, a lognormal, a weighted
average, a normal update, a Bernoulli mixture.

## Outside this design

- cost, and quantities other than time
- choosing between plans, which is a separate decision problem built
  on these forecasts
- dependencies between projects
- display and interaction

## References

Each entry ends with a status word:

- `checked`: the bibliographic details were confirmed by web search
  when the entry was written.
- `unchecked`: cited from prior knowledge. The work is well known, but
  the volume, page and year should be confirmed before relying on them
  formally.
- `read`: the cited figures were read in the paper itself.

- Aranda 2005. Aranda, J., Easterbrook, S. "Anchoring and adjustment
  in software estimation." Proceedings of ESEC/FSE 2005, 346–355.
  doi:10.1145/1081706.1081761. Findings were taken from secondary
  accounts. checked
- Arrow 1951. Arrow, K., Harris, T., Marschak, J. "Optimal inventory
  policy." Econometrica 19(3), 250–272. The newsvendor result.
  unchecked
- Boehm 1981. Boehm, B. Software Engineering Economics. Prentice
  Hall. unchecked
- Brier 1950. Brier, G. "Verification of forecasts expressed in terms
  of probability." Monthly Weather Review 78(1), 1–3. unchecked
- Buehler 1994. Buehler, R., Griffin, D., Ross, M. "Exploring the
  'planning fallacy': why people underestimate their task completion
  times." Journal of Personality and Social Psychology 67(3), 366–381.
  Study 1 figures read from Table 1. checked, read
- Clark 1961. Clark, C. "The greatest of a finite set of random
  variables." Operations Research 9(2), 145–162. checked
- Cohn 2005. Cohn, M. Agile Estimating and Planning. Prentice Hall.
  unchecked
- Connolly 1997. Connolly, T., Dean, D. "Decomposed versus holistic
  estimates of effort required for software writing tasks."
  Management Science 43(7), 1029–1045. The figures quoted are as
  reported in Jørgensen 2004. checked
- Dawes 1988. Dawes, R. Rational Choice in an Uncertain World.
  Harcourt Brace Jovanovich. Cited here as cited in Jørgensen 2004.
  unchecked
- Dechter 1991. Dechter, R., Meiri, I., Pearl, J. "Temporal constraint
  networks." Artificial Intelligence 49, 61-95. The simple temporal
  problem and its consistency condition. checked
- Fenton 2018. Fenton, N., Neil, M. Risk Assessment and Decision
  Analysis with Bayesian Networks, second edition. CRC Press. checked
- Flyvbjerg 2002. Flyvbjerg, B., Holm, M., Buhl, S. "Underestimating
  costs in public works projects: error or lie?" Journal of the
  American Planning Association 68(3), 279–295. The "about nine of
  ten" figure is from summaries. The dissent is Love, P.,
  Ahiaga-Dagbui, D., Transportation Research Part A 113 (2018). checked
- Flyvbjerg 2006. Flyvbjerg, B. "From Nobel Prize to project
  management: getting risks right." Project Management Journal 37(3),
  5–15. checked
- Gelman 2013. Gelman, A., Carlin, J., Stern, H., Dunson, D.,
  Vehtari, A., Rubin, D. Bayesian Data Analysis, third edition. CRC
  Press. Normal models, hierarchical models and partial pooling.
  unchecked
- Gneiting 2007. Gneiting, T., Raftery, A. "Strictly proper scoring
  rules, prediction, and estimation." Journal of the American
  Statistical Association 102(477), 359–378. checked
- Goldratt 1997. Goldratt, E. Critical Chain. North River Press.
  checked
- Goodhart 1975. Goodhart, C. "Problems of monetary management: the
  U.K. experience." Papers in Monetary Economics, Reserve Bank of
  Australia. unchecked
- Grenning 2002. Grenning, J. "Planning poker, or how to avoid
  analysis paralysis while release planning." White paper. unchecked
- Halkjelsvik 2012. Halkjelsvik, T., Jørgensen, M. "From origami to
  software development: a review of studies on judgment-based
  predictions of performance time." Psychological Bulletin 138(2),
  238–271. checked
- Herroelen 2001. Herroelen, W., Leus, R. "On the merits and pitfalls
  of critical chain scheduling." Journal of Operations Management
  19(5), 559-577. checked
- Herroelen 2005. Herroelen, W., Leus, R. "Project scheduling under
  uncertainty: survey and research potentials." European Journal of
  Operational Research 165(2), 289-306. checked
- Jaynes 2003. Jaynes, E. T. Probability Theory: The Logic of Science.
  Cambridge University Press. The rule of succession. unchecked
- Jørgensen 2004. Jørgensen, M., Teigen, K., Moløkken, K. "Better sure
  than safe? Over-confidence in judgement based software development
  effort prediction intervals." Journal of Systems and Software 70.
  Hit rates and the "more than 100 projects, 3 with comparable
  records" finding read in Study A. Pages unchecked. checked, read
- Kahn 1962. Kahn, A. "Topological sorting of large networks."
  Communications of the ACM 5(11), 558-562. checked
- Kahneman 1979. Kahneman, D., Tversky, A. "Intuitive prediction:
  biases and corrective procedures." TIMS Studies in Management
  Science 12, 313–327. unchecked
- Kahneman 1993. Kahneman, D., Lovallo, D. "Timid choices and bold
  forecasts: a cognitive perspective on risk taking." Management
  Science 39(1), 17–31. checked
- Kelley 1959. Kelley, J., Walker, M. "Critical-path planning and
  scheduling." Proceedings of the Eastern Joint Computer Conference,
  160-173. checked
- Kim 2009. Kim, B.-C., Reinschmidt, K. "Probabilistic forecasting of
  project duration using Bayesian inference and the beta
  distribution." Journal of Construction Engineering and Management
  135(3), 178–186. checked
- Kruger 2004. Kruger, J., Evans, M. "If you don't want to be late,
  enumerate: unpacking reduces the planning fallacy." Journal of
  Experimental Social Psychology 40(5), 586–598. Bibliographic details
  checked; findings from the title and secondary accounts
- Larrick 2006. Larrick, R., Soll, J. "Intuitions about combining
  opinions: misappreciation of the averaging principle." Management
  Science 52(1), 111–127. checked
- Little 2006. Little, T. "Schedule estimation and uncertainty
  surrounding the cone of uncertainty." IEEE Software 23(3). The 106
  projects, the doubling and the lognormal fit are from secondary
  accounts. Bibliographic details checked
- MacCrimmon 1964. MacCrimmon, K., Ryavec, C. "An analytical study of
  the PERT assumptions." Operations Research 12(1), 16-37. checked
- Malcolm 1959. Malcolm, D., Roseboom, J., Clark, C., Fazar, W.
  "Application of a technique for research and development program
  evaluation." Operations Research 7(5), 646-669. checked
- McConnell 2006. McConnell, S. Software Estimation: Demystifying the
  Black Art. Microsoft Press. unchecked
- Moløkken 2003. Moløkken, K., Jørgensen, M. "A review of surveys on
  software effort estimation." Proceedings of ISESE 2003, IEEE. The
  figures are from the abstract. checked
- Parkinson 1955. Parkinson, C. N. "Parkinson's law." The Economist,
  19 November 1955. unchecked
- Rosen 1970. Rosen, S., Tesser, A. "On reluctance to communicate
  undesirable information: the MUM effect." Sociometry 33(3),
  253–263. unchecked
- Roy 2005. Roy, M., Christenfeld, N., McKenzie, C. "Underestimating
  the duration of future events: memory incorrectly used or memory
  bias?" Psychological Bulletin 131. Findings from secondary accounts.
  Pages unchecked
- Spolsky 2007. Spolsky, J. "Evidence based scheduling." Joel on
  Software, October 2007. Described from secondary accounts. Exact
  date unchecked
- Teigen 2005. Teigen, K., Jørgensen, M. "When 90% confidence
  intervals are 50% certain: on the credibility of credible
  intervals." Applied Cognitive Psychology 19, 455–475. The finding is
  taken from the title and secondary accounts. checked
- Tversky 1974. Tversky, A., Kahneman, D. "Judgment under
  uncertainty: heuristics and biases." Science 185(4157), 1124–1131.
  unchecked
- Van Slyke 1963. Van Slyke, R. "Monte Carlo methods and the PERT
  problem." Operations Research 11(5), 839-860. checked
