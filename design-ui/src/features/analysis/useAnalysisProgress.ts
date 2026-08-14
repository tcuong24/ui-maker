import { useEffect, useState } from 'react'
import { getAnalysis, type AnalysisDetail } from './analysisApi'

const FINISHED = new Set(['COMPLETED', 'FAILED', 'CANCELLED'])

export function useAnalysisProgress(analysisId: string) {
  const [analysis, setAnalysis] = useState<AnalysisDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!analysisId) return
    const controller = new AbortController()
    let timer: ReturnType<typeof setTimeout> | undefined

    async function poll() {
      try {
        const current = await getAnalysis(analysisId, controller.signal)
        setAnalysis(current)
        setError(null)
        if (!FINISHED.has(current.status)) timer = setTimeout(poll, 1500)
      } catch (reason) {
        if (controller.signal.aborted) return
        setError(reason instanceof Error ? reason.message : 'Không thể cập nhật tiến trình')
        timer = setTimeout(poll, 5000)
      }
    }

    void poll()
    return () => { controller.abort(); if (timer) clearTimeout(timer) }
  }, [analysisId])

  return { analysis, error }
}
