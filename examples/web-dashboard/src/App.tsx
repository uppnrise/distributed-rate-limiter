import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { useLocation } from "@/lib/router";
import { AppProvider } from "./contexts/AppContext";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { DashboardLayout } from "./components/layout/DashboardLayout";
import Dashboard from "./pages/Dashboard";
import Algorithms from "./pages/Algorithms";
import Configuration from "./pages/Configuration";
import Scheduling from "./pages/Scheduling";
import LoadTesting from "./pages/LoadTesting";
import Analytics from "./pages/Analytics";
import ApiKeys from "./pages/ApiKeys";
import Adaptive from "./pages/Adaptive";
import NotFound from "./pages/NotFound";

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

  return Page ? (
    <DashboardLayout>
      <Page />
    </DashboardLayout>
  ) : (
    <NotFound />
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
