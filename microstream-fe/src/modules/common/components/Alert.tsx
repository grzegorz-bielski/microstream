import {
  Alert as ChakraAlert,
  AlertProps,
  AlertIcon,
  AlertTitle,
  AlertDescription,
} from "@chakra-ui/react"

export namespace Alert {
  export type Props = {
    status: NonNullable<AlertProps["status"]>
    title: string
    description?: string
  }
}

export function Alert({ status, title, description }: Alert.Props) {
  return (
    <ChakraAlert
      status={status}
      variant="subtle"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      textAlign="center"
      height="200px"
    >
      <AlertIcon boxSize="40px" mr={0} />
      <AlertTitle mt={4} mb={1} fontSize="lg">
        {title}
      </AlertTitle>
      {description && <AlertDescription maxWidth="sm">{description}</AlertDescription>}
    </ChakraAlert>
  )
}
