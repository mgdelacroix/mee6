# Mee6

    Rick: This is a Meeseeks Box, let me show you how it works. You press this...
    Mr. Meeseeks: I'm Mr. Meeseeks! Look at me!
    Rick: You make a request. Mr. Meeseeks, open Jerry's stupid mayonaise jar
    Mr. Meeseeks: Yesiree!
    Rick: The Meeseeks fulfills the request...
    Mr. Meeseeks: All done!
    Rick: ...and then he stops existing.
    [...]
    Rick: Just keep your requests simple. [burps] They're not gods.
-- Rick

## Rationale

`Mee6` is a simple monitoring system. Inspired by Ansible's
simplicity, it is distributed as a `jar` file that only needs the
`JVM` to be run, and it reads a simple `yaml` file where the checks,
hosts and emails to notify are specified.

`Mee6` connects to the remote hosts using plain `ssh`, and those can
be described either in the `Mee6` `yaml` file or in the `ssh` config
file.

Each check uses a module to specify which kind of task it needs to
perform. Every module has its own configuration parameters and its own
way of gathering information and producing output. A check can report
`green`, `red` or `grey` status, meaning `SUCCESS`, `FAILURE` or
`CANNOT CHECK` (failure to perform the check itself) respectively.

At the moment, only `disk-space`, `service` and `script` are
defined. First two perform a specific task, and the latter uploads any
script in the filesystem to the remote host, runs it, saves the exit
code and parses its output to decide what's its status and what info
it should show.

Besides the capability of sending emails to notify of any status
change, it has a simple web interface to visually show the status of
all the checks.

`Mee6` will send an email to the `notify` group of a check if the
check is not `green` when the tool starts, or if it status changes.

## Screenshots

![Mee6](https://i.ytimg.com/vi/QrQZg-gNC_k/maxresdefault.jpg)

## Execution and installation

To run `Mee6`, you just need its `jar` file and a config file. To
start executing the application, run:

```sh
MEE6_CONFIG=/path/to/my/config.yaml java -jar mee6.jar
```

If your operative system uses `systemd` to manage the services, you
can copy the `resources/mee6.service` to `/etc/systemd/system` and
replace the placeholders it has to suit your installation. Then run:

```sh
systemctl daemon-reload
systemctl start mee6
systemctl enable mee6  # to run it every time the system boots
```

### Web GUI security

`Mee6` has a web GUI that can be enabled using the `http` section of
the configuration file. This interface is by default unprotected.

To secure it, is recommended to expose the interface's port only
through a reverse proxy with a Basic Auth schema.

## Config file

The configuration file is a `yaml` file with the following blocks:

### hosts

Every host is a key inside the block. They will later be referenced by
that key:

| parameter | required | description |
|-----------|----------|-------------|
| hostname  | yes      | It can be either the `URI` of the host in the `username@host:port` format or the `ssh` config name. |
| key       | no       | The `ssh` key to use when connecting to the host. _Default:_ `nil` |

The hosts are later referenced from the checks by their name.

```yaml
hosts:
  database:
    hostname: myuser@remote_machine:2324
    key: ~/.ssh/id_rsa
  web:
    hostname: web1_myproject
```

### checks

Every check is an item inside the block.

| parameter | required | description |
|-----------|----------|-------------|
| name      | yes      | A short and descriptive name for the check. |
| hosts     | yes      | A list of the hosts to run the check on. Use the names of the hosts block. |
| cron      | yes      | A cron expression with the [Quartz syntax](www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html). |
| notify    | yes      | The notify group to write to if something happens. |
| module    | yes      | The module to run during the check. Check the [module list]() for more information. |

Each module can have its own parameters, which should be added to the
check.

```yaml
checks:
  - name: Disk usage on /dev/sda1
    hosts:
      - database
      - web
    cron: "*/5 * * * * ?"
    notify: sys
    module: disk-usage
    device: /dev/sda1
    threshold: 10

  - name: Check if nginx is up and running
    hosts:
      - database
    cron: "*/5 * * * * ?"
    notify: sys
    module: service
    service: nginx

  - name: Check debian version
    hosts:
      - raider
    cron: "*/5 * * * * ?"
    notify: test
    module: script
    file: /home/monitoring/scripts/check_debian_version.sh
    # args:
```

### notify

We can describe each notify group as a key inside the notify
block. They will later be referenced by that key:

| parameter   | required | description |
|-------------|----------|-------------|
| description | yes      | A short description of the notify group. |
| emails      | yes      | A list of the email addresses to write to in case of status change. |

```yaml
notify:
  sys:
    description: The sysops guys
    emails:
      - john.doe@example.com
      - jane.doe@example.com
  test:
    description: Testing
    emails:
      - me@example.com
```

### mail

This is where the mail configuration goes. `Mee6` uses
the [postal library](https://github.com/drewr/postal) to notify. As
for now, it can be configured in three modes:

- local: Uses the `sendmail-like` method local to the machine where
  it's running.
- console: Writes the emails to the console instead of sending them.
- smtp: Uses the described smtp server to send the emails.

| parameter | required | description |
|-----------|----------|-------------|
| from      | yes      | The `from` to use when sending the notification. |
| mode      | yes      | One of `local`, `console` or `smtp`, described above. |
| host      | no       | The host for of `smtp`. Required if `mode = smtp`. |
| user      | no       | The user for of `smtp`. |
| pass      | no       | The password for of `smtp`. |
| ssl       | no       | Enable `ssl` mode for the `smtp` connection. Cannot be used if `tls` is `true`. |
| tls       | no       | Enable `tls` mode for the `smtp` connection. Cannot be used if `ssl` is `true`. |
| port      | no       | Port for the `smtp` connection. |

#### Console example

```yaml
mail:
  from: mee6@example.com
  mode: console
```

#### Local example

```yaml
mail:
  from: mee6@example.com
  mode: local
```

#### SMTP example (gmail)

```yaml
mail:
  from: mee6@example.com
  mode: smtp
  host: smtp.gmail.com
  user: myusername
  pass: mypassword
  ssl: true
```

### http

This block configures the web GUI. If the port is not set, the web
will be disabled.

| parameter | required | description |
|-----------|----------|-------------|
| port      | no       | The port for the embedded webserver. |

```yaml
http:
  port: 3001
```

## Modules

### disk-usage

| parameter | required | description |
|-----------|----------|-------------|
| device    | yes      | The device to look for in the `df -l` stdout. |
| threshold | no       | The usage percentage that, if it's trespassed, triggers an alert and a `FAILED` state for the check. |

### service

| parameter | required | description |
|-----------|----------|-------------|
| service   | yes      | The name of the `systemd` service to check. |

### script

| parameter | required | description |
|-----------|----------|-------------|
| file      | yes      | The script to upload and execute in the remote machine. |
| args      | no       | The arguments to pass to the remote script. |

## Development
