# logback-spring.xml explained

This file is the application’s logging setup. It tells Spring Boot/Logback:

- where logs should go
- what format they should use
- when to print to console vs save to files
- how to rotate files when they get too large
- which metadata to attach to each log line

Simple idea:
- Console logs = easy to read while developing
- File logs = good for production/debugging
- JSON logs = easier for log aggregation tools like Kibana, ELK, Splunk, etc.

## 1) Overall structure

This is a Logback XML config file. Logback is the logging engine used by Spring Boot.

The file starts with:

```xml
<configuration>
    ...
</configuration>
```

This means: “this whole file defines how logging behaves.”

Then it defines:
- appenders: destinations for logs
- loggers: categories of logs
- properties: paths like `logs/api.log`
- profiles: environment-specific behavior

## 2) Profiles: dev/stg/pre vs everything else

This is the most important part:

```xml
<springProfile name="dev|stg|pre">
```

For dev, staging, and pre-production:
- logs go to the console in a readable raw format
- logs also go to JSON files

For other profiles like prod or perf:
- no console output
- logs go only to JSON files

This is controlled by Spring profile names. In simple terms:
- “If the app is running in dev/stg/pre, show me logs in the terminal”
- “If it is running elsewhere, keep console quiet and store logs only in files”

## 3) STDOUT appender

This section creates a console logger for normal text output:

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>[uniqueLogId=%X{uniqueLogId},parentId=%X{parentId}]${CONSOLE_LOG_PATTERN}</pattern>
        <charset>utf8</charset>
    </encoder>
</appender>
```

It writes to the terminal and uses a pattern like:

```text
[uniqueLogId=...,parentId=...] ...
```

This means each console log includes MDC values like `uniqueLogId` and `parentId`.

This is the “human-readable log output”.

## 4) STDOUT_JSON appender

This is the JSON console logger:

```xml
<appender name="STDOUT_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
```

It converts log events into JSON so they are machine-readable.

Included JSON fields:
- log level
- thread name
- logger name
- arguments
- message
- MDC fields:
  - traceId
  - spanId
  - parentId
  - uniqueLogId

This is useful for structured logging in centralized systems.

## 5) Why MDC matters

MDC = Mapped Diagnostic Context.

It adds context values to logs automatically, such as:
- traceId
- spanId
- parentId
- uniqueLogId

This helps trace one request across multiple services or methods. If a request moves through several components, you can follow the same trace.

## 6) API log

This creates a file-based JSON logger for API logs:

```xml
<property name="API_LOG_PATH" value="logs/api.log"/>
```

It writes to `logs/api.log`.

Then it uses a rolling file appender:

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>logs/api/api.%d{yyyy-MM-dd-HH}.%i.log</fileNamePattern>
    <maxFileSize>1GB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>30GB</totalSizeCap>
</rollingPolicy>
```

This means:
- every hour, a new log file is created
- if a file becomes too large, it rotates
- logs are kept for 30 periods
- total storage is capped at 30GB

Then:

```xml
<logger name="apiLogger" additivity="false" level="INFO">
    <springProfile name="dev|stg|pre">
        <appender-ref ref="STDOUT_JSON"/>
    </springProfile>
    <appender-ref ref="apiJsonAppender"/>
</logger>
```

This means:
- API logs are isolated from general logs
- they are JSON
- in local envs, they are also printed to console
- they always get saved to the file

## 7) BACKEND log

Same idea as API log, but for backend logs.

```xml
<property name="BACKEND_LOG_PATH" value="logs/backend.log"/>
```

It logs to `logs/backend.log` and creates rotated files like:

```text
logs/backend/backend.<date>.<index>.log
```

It also writes to console in dev/stg/pre and saves to a file always.

This separates backend system logs from API logs.

## 8) DETAIL log

This is for detailed logging and troubleshooting.

```xml
<property name="DETAIL_LOG_PATH" value="logs/detail.log"/>
```

It includes:
- `@timestamp`
- message
- thread name
- MDC values

The timestamp format is:

```xml
<pattern>yyyy-MM-dd'T'HH:mm:ss.SSSZ</pattern>
```

And timezone is:

```xml
<timeZone>Asia/Tokyo</timeZone>
```

This logger is useful for deep debugging, not normal app logs.

## 9) INFO and ERROR log

These are the main general logs.

```xml
<property name="INFO_LOG_PATH" value="logs/info.log"/>
<property name="ERROR_LOG_PATH" value="logs/error.log"/>
```

The info logger records normal informational messages.
The error logger only captures ERROR-level logs because of:

```xml
<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
    <level>ERROR</level>
</filter>
```

So `error.log` is dedicated to exceptions and critical failures.

These logs include:
- timestamp
- log level
- logger name
- message
- thread name
- MDC IDs
- stack trace

That is extremely helpful for production debugging.

## 10) Root logger and how it connects with other loggers

At the end of the file:

```xml
<springProfile name="dev|stg|pre">
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</springProfile>
```

This means: in dev/stg/pre, all logs at INFO or above go to the console.

Then:

```xml
<root level="INFO">
    <appender-ref ref="infoJsonAppender"/>
    <appender-ref ref="errorJsonAppender"/>
</root>
```

This means: for all environments, logs go to `info.log` and `error.log` in JSON format.

### Root logger is the default catch-all

The root logger is the parent of all loggers in Logback. If you do not define a logger for a specific category, it inherits from root.

So this is the general flow:

- specific logger
- parent logger
- root logger

Each logger can have:
- a level
- appenders
- additivity

If additivity is true (default), a log event is passed to parent loggers too.
If additivity is false, it stops there.

### Why root matters here

In your file:

- `apiLogger` has `additivity="false"`
- `backendLogger` has `additivity="false"`
- `detailLogger` has `additivity="false"`

That means these loggers do not pass their events to root.

So they go only to their own appender(s):
- apiLogger → apiJsonAppender + STDOUT_JSON in dev/stg/pre
- backendLogger → backendJsonAppender + STDOUT_JSON in dev/stg/pre
- detailLogger → detailJsonAppender + STDOUT_JSON in dev/stg/pre

But ordinary app logs, framework logs, and library logs do not use those named loggers, so they reach root and get written to:
- infoJsonAppender
- errorJsonAppender
- plus STDOUT in dev/stg/pre

### Simple example

If code does this:

```java
Logger apiLog = LoggerFactory.getLogger("apiLogger");
apiLog.info("request completed");
```

This log goes to:
- apiJsonAppender
- and in dev/stg/pre also to STDOUT_JSON

It does NOT go to root because `additivity="false"`.

But if code does this:

```java
Logger log = LoggerFactory.getLogger(HomeController.class);
log.info("Home page requested");
```

There is no logger named `HomeController` specifically configured, so it inherits from root.

That log goes to:
- infoJsonAppender
- errorJsonAppender
- and in dev/stg/pre also to STDOUT

## 11) Edge cases and scenarios

### Edge case 1: logger with additivity=true

If a logger is configured like this:

```xml
<logger name="customLogger" level="INFO" additivity="true">
    <appender-ref ref="someAppender"/>
</logger>
```

Then the log event is sent to:
- customAppender
- and then also to root

That can create duplicate logs.

### Edge case 2: child logger under a package

If you define:

```xml
<logger name="com.faysal.allexistingtechs" level="INFO">
```

then all child loggers under `com.faysal.allexistingtechs` inherit from it.

If `additivity` stays true, those logs may be sent to:
- this package logger appender
- then also to root

This is a common reason for duplicated entries in logs.

### Edge case 3: logger names do not match anything

If code uses:

```java
LoggerFactory.getLogger("someRandomLogger");
```

and no matching logger is configured, it falls back to root.

That means it goes to the root appenders only.

### Edge case 4: error logs reaching root from frameworks and libraries

Frameworks such as Spring, Hibernate, or other libraries often log at `ERROR` or `WARN`.
If they do not have specific logger configuration, those logs go to root.

That means an exception from a library can appear in:
- `error.log`
- and in console in dev/stg/pre

Even if the code that triggered it was not using `apiLogger` or `backendLogger`.

## 12) Why the file is structured this way

This config intentionally separates logs into categories:

- `apiLogger` → API logs
- `backendLogger` → backend system logs
- `detailLogger` → detailed diagnostics
- root → general application/framework logs

This is useful because:
- API logs can be monitored separately
- backend logs stay isolated
- debugging can use detail logs without mixing with business logs
- root remains the catch-all for everything else

## 13) Very simple mental model

Think of this config like this:

- `apiLogger` is a special basket for API events
- `backendLogger` is a special basket for backend events
- `detailLogger` is a special basket for debug events
- root is the default basket for everything else

And `additivity=false` means: “do not also throw this item into the default basket.”

## 14) Why this is good for production

This setup makes logs easier to manage because:
- critical errors are centralized in `error.log`
- general info logs are centralized in `info.log`
- specific concerns are separated into their own files
- dev environments remain easy to read in the console
- log files can rotate automatically to prevent disk blow-ups

## 15) In one sentence

The root logger is the default destination for all logs that do not have a more specific logger configuration, while the named loggers in this file intentionally stop at their own appenders to keep API/backend/detail logs isolated from the main root logs.

If you want, I can also turn this into a visual flow diagram showing the logger hierarchy and appenders in one quick sketch.
