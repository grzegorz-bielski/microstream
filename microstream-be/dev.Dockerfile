FROM mozilla/sbt

# install deps

WORKDIR /microstream-be

RUN mkdir -p ./project
RUN mkdir -p ./src/main/resources

COPY build.sbt .
COPY ./project/* ./project/
COPY ./src/main/resources/* ./src/main/resources/
RUN sbt compile

EXPOSE 2552 8080 8558 5432

COPY . .
CMD sbt compile run