import { lazy, Suspense } from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { useLocation } from "@/lib/router";
import { AppProvider } from "./contexts/AppContext";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { DashboardLayout } from "./components/layout/DashboardLayout";
const Dashboard = lazy(() => import("./pages/Dashboard"));
const Algorithms = lazy(() => import("./pages/Algorithms"));
const Configuration = lazy(() => import("./pages/Configuration"));
const Scheduling = lazy(() => import("./pages/Scheduling"));
const LoadTesting = lazy(() => import("./pages/LoadTesting"));
const Analytics = lazy(() => import("./pages/Analytics"));
const ApiKeys = lazy(() => import("./pages/ApiKeys"));
const Adaptive = lazy(() => import("./pages/Adaptive"));
const NotFound = lazy(() => import("./pages/NotFound"));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 3,
      staleTime: 30000,
    },
  },
});

const routes = {
  "/": Dashboard,
  "/algorithms": Algorithms,
  "/configuration": Configuration,
  "/scheduling": Scheduling,
  "/load-testing": LoadTesting,
  "/analytics": Analytics,
  "/api-keys": ApiKeys,
  "/adaptive": Adaptive,
} as const;

const AppRoute = () => {
  const { pathname } = useLocation();
  const Page = routes[pathname as keyof typeof routes];

  return (
    <Suspense fallback={<div className="min-h-screen bg-background" aria-label="Loading page" />}>
      {Page ? (
        <DashboardLayout>
          <Page />
        </DashboardLayout>
      ) : (
        <NotFound />
      )}
    </Suspense>
  );
};

const App = () => (
  <ErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
        <AppProvider>
          <TooltipProvider>
            <Toaster />
            <Sonner />
            <AppRoute />
          </TooltipProvider>
        </AppProvider>
      </ThemeProvider>
    </QueryClientProvider>
  </ErrorBoundary>
);

export default App;
