# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.2.

## Development server

### Standard (localhost)

For local development on the same machine:

```bash
npm start
```

Open `http://localhost:4200/` in your browser. The application reloads automatically on file changes.

### Mobile / LAN access

To access the app from a mobile device or another device in the same network:

```bash
npm run start:mobile
```

Open `http://192.168.1.101:4200/` on the mobile device. API calls are directed to `http://192.168.1.101:8080`.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
npx ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
npx ng generate --help
```

## Building

To build the project run:

```bash
npm run build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
npm test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
npx ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
