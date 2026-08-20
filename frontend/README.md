# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.19.

## Development server

The UI takes the company from the hostname (`acme.localhost` → `acme`) and calls the API on the **same hostname**, port `8080`.

1. Add this line to your hosts file (`C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on macOS/Linux):

   ```
   127.0.0.1 acme.localhost globex.localhost
   ```

2. Start the Spring Boot API (listens on `8080`).
3. From this `frontend` folder:

   ```bash
   npm start
   ```

4. Open [http://localhost:4200](http://localhost:4200), click **Log in**, enter company domain `acme` (or `globex`). You are sent to that company’s login page.

   - Acme: [http://acme.localhost:4200/login](http://acme.localhost:4200/login) — `alice.morgan@company.com` / `password`
   - Globex: [http://globex.localhost:4200/login](http://globex.localhost:4200/login) — `emma.frost@globex.com` / `password`

Invite links must be opened on that company’s host, for example `http://acme.localhost:4200/invite?token=...`.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
