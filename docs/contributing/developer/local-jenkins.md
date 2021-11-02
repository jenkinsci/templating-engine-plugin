# Running A Local Jenkins

It's often helpful to run Jenkins in a container locally to test various scenarios with JTE during development.

``` bash
just run 
```

With the default settings, this will expose jenkins on [http://localhost:8080](http://localhost:8080)

## Change the container name

``` bash
just --set container someName run
```

## Change the port forwarding target

``` bash
just --set port 9000 run
```

## Pass arbitrary flags to the container

Parameters passed to `just run` are sent as flags to the `docker run` command.

``` bash
just run -e SOMEVAR="some var"
```

## Mounting local libraries for testing

Local directories can be configured as Git SCM Library Sources even if they don't have a remote repository.

For example, if `~/local-libraries` is a directory containing a local git repository then to mount it to the container you would run:

``` bash
just run -v ~/local-libraries:/local-libraries 
```

You could then configure a Library Source using the file protocol to specify the repository location at `file:///local-libraries`

<!-- markdownlint-disable -->
!!! tip 
    When using this technique, changes to the libraries must be committed to be found. In a separate terminal, run:

    ``` bash
    just watch ~/local-libraries
    ```

    to automatically commit changes to the libraries. 
<!-- markdownlint-restore -->