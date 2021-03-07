module.exports = {
    async redirects() {
      return [
        {
          source: '/',
          destination: '/channels',
          permanent: true,
        },
      ]
    },
  }