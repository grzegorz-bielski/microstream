import { Box, Heading, ListItem, List } from "@chakra-ui/layout"
import { pipe } from "fp-ts/lib/function"
import { useChannels } from "modules/channels/channels"
import * as R from "@devexperts/remote-data-ts"
import { Alert } from "modules/common/components/Alert"

export default function Channels() {
  const channels = useChannels()

  console.log("channels", channels)

  return (
    <Box>
      <Heading as="h2">Hublasbublas!!!!XDDDddddd</Heading>
      {pipe(
        channels,
        R.fold3(
          () => <Alert status="info" title="No data so far 👀" />,
          (e) => <Alert status="error" title="An error has occurred!" description={e.message} />,
          (channels) => (
            <List>
              {channels.map((c) => (
                <ListItem key={c.name}>{c.name}</ListItem>
              ))}
            </List>
          ),
        ),
      )}
    </Box>
  )
}
