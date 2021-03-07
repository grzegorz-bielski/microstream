import useSwr from "swr"
import * as R from "@devexperts/remote-data-ts"
import * as t from "io-ts"

const naiveFetcher = <T>(input: RequestInfo, init?: RequestInit | undefined): Promise<T> =>
  fetch(input, init)
    .then((res) => res.json())
    .then((x) => {
      // todo: decode
      console.log("x", x)
      return x
    })

const Channel = t.type({
  name: t.string,
})
type Channel = t.TypeOf<typeof Channel>

type AppError = Error

export function useChannels(): R.RemoteData<AppError, Channel[]> {
  const result = useSwr<Channel[], AppError>("http://localhost:8080/api/channels", (url) =>
    naiveFetcher(url),
  )

  const { data, error } = result

  if (error) {
    return R.failure(error)
  }

  if (data) {
    return R.success(data)
  }

  return R.pending
}
