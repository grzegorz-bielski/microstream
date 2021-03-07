import { ChakraProvider, Link } from "@chakra-ui/react"
import { Box, Heading } from "@chakra-ui/layout"
import { AppProps } from "next/app"
import NextLink from "next/link"
import Head from "next/head"

export default function MyApp({ Component, pageProps }: AppProps) {
  return (
    <ChakraProvider>
      <Head>
        <title>microstream</title>
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <Box>
        <Heading as="h1" size="sm">
          <Link as={NextLink} href="/">
            microstream.ch
          </Link>
        </Heading>
      </Box>
      <Component {...pageProps} />
    </ChakraProvider>
  )
}
