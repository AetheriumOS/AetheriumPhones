package net.aetherium.phones.audio

/**
 * Simple detector for the hybrid window logic (7 s).
 */
object HybridDetector {
    const val WINDOW_MS = 7_000L
    enum class Verdict { UNKNOWN, USES_AETH_NOW, NOT_USING_AETH_NOW, USED_BUT_NOT_NOW }
    data class State(var startTime: Long = System.currentTimeMillis(), var sawAeth: Boolean = false, var sawNonAeth: Boolean = false, var conflict: Boolean = false, var verdict: Verdict = Verdict.UNKNOWN)
    fun begin(): State = State()
    fun onAethBeacon(state: State) {
        if (System.currentTimeMillis() - state.startTime > WINDOW_MS) return
        if (!state.sawNonAeth && !state.conflict) {
            state.sawAeth = true
            state.verdict = Verdict.USES_AETH_NOW
        } else {
            state.conflict = true
            state.verdict = Verdict.NOT_USING_AETH_NOW
        }
    }
    fun onNonAethBeacon(state: State) {
        if (System.currentTimeMillis() - state.startTime > WINDOW_MS) return
        if (!state.sawAeth) {
            state.sawNonAeth = true
            state.verdict = Verdict.NOT_USING_AETH_NOW
        } else {
            state.conflict = true
            state.verdict = Verdict.NOT_USING_AETH_NOW
        }
    }
    fun endWindow(state: State) {
        if (state.verdict == Verdict.UNKNOWN) {
            state.verdict = if (state.sawAeth && !state.conflict) Verdict.USES_AETH_NOW else Verdict.NOT_USING_AETH_NOW
        }
    }
}
