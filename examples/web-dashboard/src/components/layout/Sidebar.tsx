import { NavLink } from "react-router-dom";
import { 
  Home, 
  Settings, 
  Sliders, 
  Zap, 
  BarChart3, 
  Key,
  Calendar,
  ChevronLeft,
  Brain
} from "lucide-react";
import { cn } from "@/lib/utils";

interface SidebarProps {
  open: boolean;
  onToggle: () => void;
}

const navItems = [
  { title: "Dashboard", url: "/", icon: Home },
  { title: "Adaptive", url: "/adaptive", icon: Brain },
  { title: "Algorithms", url: "/algorithms", icon: Settings },
  { title: "Configuration", url: "/configuration", icon: Sliders },
  { title: "Scheduling", url: "/scheduling", icon: Calendar },
  { title: "Load Testing", url: "/load-testing", icon: Zap },
  { title: "Analytics", url: "/analytics", icon: BarChart3 },
  { title: "API Keys", url: "/api-keys", icon: Key },
];

export const Sidebar = ({ open, onToggle }: SidebarProps) => {
  return (
    <aside
      className={cn(
        "relative flex flex-col border-r border-sidebar-border bg-sidebar-background transition-all duration-300",
        open ? "w-64" : "w-20"
      )}
      role="navigation"
      aria-label="Main navigation"
    >
      <div className="flex h-16 items-center justify-between border-b border-sidebar-border px-4">
        {open && (
          <span className="bg-gradient-to-r from-primary to-accent bg-clip-text text-lg font-bold text-transparent">
            Rate Limiter
          </span>
        )}
        <button
          onClick={onToggle}
          className="rounded-lg p-2 text-sidebar-foreground transition-colors hover:bg-sidebar-accent focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
          aria-label={open ? "Collapse sidebar" : "Expand sidebar"}
          aria-expanded={open}
        >
          <ChevronLeft
            className={cn(
              "h-5 w-5 transition-transform duration-300",
              !open && "rotate-180"
            )}
            aria-hidden="true"
          />
        </button>
      </div>

      <nav className="flex-1 space-y-1 p-3">
        {navItems.map((item) => (
          <NavLink
            key={item.url}
            to={item.url}
            end={item.url === "/"}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2",
                isActive
                  ? "bg-sidebar-accent text-sidebar-accent-foreground"
                  : "text-sidebar-foreground hover:bg-sidebar-accent/50"
              )
            }
            title={!open ? item.title : undefined}
          >
            {({ isActive }) => (
              <>
                <item.icon className="h-5 w-5 flex-shrink-0" aria-hidden="true" />
                {open && <span>{item.title}</span>}
                {!open && <span className="sr-only">{item.title}</span>}
                {isActive && <span className="sr-only">(current page)</span>}
              </>
            )}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
};
